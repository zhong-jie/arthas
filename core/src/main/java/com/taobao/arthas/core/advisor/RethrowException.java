package com.taobao.arthas.core.advisor;

/**
 * @author zhongjie
 * @since 2024-09-27
 */
public class RethrowException extends Exception {

    public RethrowException(RuntimeException rethrowException) {
        this.rethrowException = rethrowException;
    }

    private final RuntimeException rethrowException;

    public RuntimeException getRethrowException() {
        return rethrowException;
    }
}
