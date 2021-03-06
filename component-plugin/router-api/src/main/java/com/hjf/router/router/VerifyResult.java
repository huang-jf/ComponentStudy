package com.hjf.router.router;

public class VerifyResult {
    private final boolean isSuccess;

    private Throwable throwable;

    public VerifyResult(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public VerifyResult(boolean isSuccess, Throwable throwable) {
        this.isSuccess = isSuccess;
        this.throwable = throwable;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}
