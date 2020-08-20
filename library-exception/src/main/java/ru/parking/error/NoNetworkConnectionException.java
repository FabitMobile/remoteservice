package ru.parking.error;


public class NoNetworkConnectionException extends Exception {

    public NoNetworkConnectionException() {
        super();
    }

    public NoNetworkConnectionException(String detailMessage) {
        super(detailMessage);
    }
}