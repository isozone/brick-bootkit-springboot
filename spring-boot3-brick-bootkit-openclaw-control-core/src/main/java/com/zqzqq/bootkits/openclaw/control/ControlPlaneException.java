package com.zqzqq.bootkits.openclaw.control;

public class ControlPlaneException extends RuntimeException {

    public ControlPlaneException(String message) {
        super(message);
    }

    public ControlPlaneException(String message, Throwable cause) {
        super(message, cause);
    }
}
