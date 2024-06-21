/*
 * Copyright [2013-2021], Alibaba Group Holding Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.polardbx.executor.mpp.server.remotetask;

import com.alibaba.polardbx.common.exception.TddlRuntimeException;
import com.alibaba.polardbx.common.exception.code.ErrorCode;
import com.alibaba.polardbx.common.utils.logger.Logger;
import com.alibaba.polardbx.common.utils.logger.LoggerFactory;
import com.alibaba.polardbx.executor.mpp.execution.TaskId;
import com.alibaba.polardbx.executor.mpp.metadata.TaskLocation;
import com.alibaba.polardbx.executor.mpp.util.Failures;
import com.google.common.collect.ObjectArrays;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import io.airlift.event.client.ServiceUnavailableException;

import javax.annotation.concurrent.ThreadSafe;
import java.io.EOFException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;

import static com.alibaba.polardbx.common.exception.code.ErrorCode.ERR_REMOTE_TASK;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

@ThreadSafe
public class RequestErrorTracker {
    private static final Logger log = LoggerFactory.getLogger(RequestErrorTracker.class);

    private final TaskId taskId;
    private final TaskLocation taskUri;
    private final ScheduledExecutorService scheduledExecutor;
    private final String jobDescription;
    private final Backoff backoff;

    private final Queue<Throwable> errorsSinceLastSuccess = new ConcurrentLinkedQueue<>();

    public RequestErrorTracker(TaskId taskId, TaskLocation taskUri, long minErrorDuration, long maxErrorDuration,
                               ScheduledExecutorService scheduledExecutor, String jobDescription) {
        this.taskId = taskId;
        this.taskUri = taskUri;
        this.scheduledExecutor = scheduledExecutor;
        this.backoff = new Backoff(minErrorDuration, maxErrorDuration);
        this.jobDescription = jobDescription;
    }

    public ListenableFuture<?> acquireRequestPermit() {
        long delayMills = backoff.getBackoffDelayMills();

        if (delayMills == 0) {
            return Futures.immediateFuture(null);
        }

        ListenableFutureTask<Object> futureTask = ListenableFutureTask.create(() -> null);
        scheduledExecutor.schedule(futureTask, delayMills, MILLISECONDS);
        return futureTask;
    }

    public void startRequest() {
        // before scheduling a new request clear the error timer
        // we consider a request to be "new" if there are no current failures
        if (backoff.getFailureCount() == 0) {
            requestSucceeded();
        }
    }

    public void requestSucceeded() {
        backoff.success();
        errorsSinceLastSuccess.clear();
    }

    public void requestFailed(Throwable reason) throws TddlRuntimeException {
        // cancellation is not a failure
        if (reason instanceof CancellationException) {
            return;
        }

        if (reason instanceof RejectedExecutionException) {
            throw new TddlRuntimeException(ERR_REMOTE_TASK, reason, reason.getMessage());
        }

        // log failure message
        if (isExpectedError(reason)) {
            // don't print a stack for a known errors
            log.warn("Error " + jobDescription + " " + taskId + ": " + reason.getMessage() + ": " + taskUri);
        } else {
            log.warn("Error " + jobDescription + " " + taskId + ": " + taskUri, reason);
        }

        // remember the first 10 errors
        if (errorsSinceLastSuccess.size() < 10) {
            errorsSinceLastSuccess.add(reason);
        }

        // fail the task, if we have more than X failures in a row and more than Y seconds have passed since the last request
        if (backoff.failure()) {
            // it is weird to mark the task failed locally and then cancel the remote task, but there is no way to tell a remote task that it is failed
            TddlRuntimeException exception =
                new TddlRuntimeException(ErrorCode.ERR_EXECUTE_MPP,
                    format("%s (%s %s - %s failures, time since last success %s)",
                        Failures.WORKER_NODE_ERROR,
                        jobDescription,
                        taskUri,
                        backoff.getFailureCount(),
                        backoff.getTimeSinceLastSuccess().convertTo(SECONDS)));
            errorsSinceLastSuccess.forEach(exception::addSuppressed);
            throw exception;
        }
    }

    static void logError(Throwable t, String format, Object... args) {
        if (isExpectedError(t)) {
            log.error(String.format(format + ": %s", ObjectArrays.concat(args, t)));
        } else {
            String message = String.format(format, args);
            log.error(message, t);
        }
    }

    private static boolean isExpectedError(Throwable t) {
        while (t != null) {
            if ((t instanceof SocketException) ||
                (t instanceof SocketTimeoutException) ||
                (t instanceof EOFException) ||
                (t instanceof TimeoutException) ||
                (t instanceof ServiceUnavailableException)) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
