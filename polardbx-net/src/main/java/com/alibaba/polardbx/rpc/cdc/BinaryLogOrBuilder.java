// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: DumperServer.proto

package com.alibaba.polardbx.rpc.cdc;

public interface BinaryLogOrBuilder extends
    // @@protoc_insertion_point(interface_extends:dumper.BinaryLog)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string logName = 1;</code>
   * @return The logName.
   */
  java.lang.String getLogName();
  /**
   * <code>string logName = 1;</code>
   * @return The bytes for logName.
   */
  com.google.protobuf.ByteString
      getLogNameBytes();

  /**
   * <code>int64 fileSize = 2;</code>
   * @return The fileSize.
   */
  long getFileSize();
}
