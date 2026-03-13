package com.lyh.base.agent.exception;

/*
 * @Author:  lengYinHui
 * @Date:  2026/3/13 13:15
 */
public class ModelRetryException extends RuntimeException {
    public ModelRetryException() {
        super();
    }

    public ModelRetryException(String message) {
        super(message);
    }

    public ModelRetryException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModelRetryException(Throwable cause) {
        super(cause);
    }
}
