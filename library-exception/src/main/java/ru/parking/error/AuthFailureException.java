package ru.parking.error;


public class AuthFailureException extends Exception {

    private Integer errorCode;

    public AuthFailureException() {
        super();
    }

    public AuthFailureException(String detailMessage, Integer errorCode) {
        super(detailMessage);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
