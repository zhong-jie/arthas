package com.taobao.arthas.core.command.monitor200;

import java.time.LocalDateTime;

/**
 * @author zhongjie
 * @since 2024-09-27
 */
public class RateLimitData {
    private String className;
    private String methodName;
    private int total;
    private int success;
    private int failed;
    private LocalDateTime timestamp;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public LocalDateTime getTimestamp() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
