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

package com.alibaba.polardbx.qatest.ddl.auto.localpartition;

import com.alibaba.polardbx.qatest.BinlogIgnore;
import com.alibaba.polardbx.qatest.util.JdbcUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.sql.ResultSet;
import java.sql.SQLException;

@BinlogIgnore(ignoreReason = "drop local partition的动作目前无法透传给下游，导致binlog实验室上下游数据不一致，暂时忽略")
public class LocalPartitionCheckTest extends LocalPartitionBaseTest {

    private String primaryTableName;

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Before
    public void before() {
        primaryTableName = randomTableName("t_dal", 4);
    }

    @After
    public void after() {

    }

    @Test
    public void checkWithLocalPartition() throws SQLException {
        String createTableSql = String.format("CREATE TABLE %s (\n"
            + "    c1 bigint,\n"
            + "    c2 bigint,\n"
            + "    c3 bigint,\n"
            + "    gmt_modified DATE PRIMARY KEY NOT NULL\n"
            + ")\n"
            + "PARTITION BY HASH(c1)\n"
            + "PARTITIONS 4\n"
            + ";", primaryTableName);
        JdbcUtil.executeSuccess(tddlConnection, createTableSql);

        ResultSet resultSet = JdbcUtil.executeQuery(
            String.format("check table %s with local partition", primaryTableName),
            tddlConnection
        );
        Assert.assertTrue(resultSet.next());
        Assert.assertEquals(resultSet.getString("PARTITION_DETAIL"), "Not a local partition table");
    }

    @Test
    public void checkWithLocalPartitionRename() throws SQLException {
        String createTableSql = String.format("CREATE TABLE %s (\n"
            + "    c1 bigint,\n"
            + "    c2 bigint,\n"
            + "    c3 bigint,\n"
            + "    gmt_modified DATE PRIMARY KEY NOT NULL\n"
            + ")\n"
            + "PARTITION BY HASH(c1)\n"
            + "PARTITIONS 4\n"
            + ";", primaryTableName);
        JdbcUtil.executeSuccess(tddlConnection, createTableSql);

        String newName = primaryTableName + "_wumu";
        String sql = String.format("rename table %s to %s", primaryTableName, newName);
        JdbcUtil.executeSuccess(tddlConnection, sql);

        ResultSet resultSet = JdbcUtil.executeQuery(
            String.format("check table %s with local partition", newName),
            tddlConnection
        );
        Assert.assertTrue(resultSet.next());
        Assert.assertEquals(resultSet.getString("PARTITION_DETAIL"), "Not a local partition table");
    }

    @Test
    public void checkWithLocalPartitionRename2() throws SQLException {
        String createTableSql = String.format("CREATE TABLE %s (\n"
            + "    c1 bigint,\n"
            + "    c2 bigint,\n"
            + "    c3 bigint,\n"
            + "    gmt_modified DATE PRIMARY KEY NOT NULL\n"
            + ")\n"
            + "PARTITION BY HASH(c1)\n"
            + "PARTITIONS 4\n"
            + "LOCAL PARTITION BY RANGE(gmt_modified)"
            + "INTERVAL 1 MONTH\n"
            + "EXPIRE AFTER 6\n"
            + "PRE ALLOCATE 3\n"
            + "PIVOTDATE NOW();", primaryTableName);
        JdbcUtil.executeSuccess(tddlConnection, createTableSql);

        String newName = primaryTableName + "_wumu";
        String sql = String.format("rename table %s to %s", primaryTableName, newName);
        JdbcUtil.executeSuccess(tddlConnection, sql);

        JdbcUtil.executeSuccess(tddlConnection, createTableSql);
        newName = primaryTableName + "_wumu2";
        sql = String.format("rename table %s to %s", primaryTableName, newName);
        JdbcUtil.executeSuccess(tddlConnection, sql);

        ResultSet resultSet = JdbcUtil.executeQuery(
            String.format("check table %s with local partition", newName),
            tddlConnection
        );
        Assert.assertTrue(resultSet.next());
        Assert.assertEquals(resultSet.getString("PARTITION_DETAIL"), "-");
    }

}