package ru.parking.remoteservice;


public class WrongThreadException extends RuntimeException {

    public WrongThreadException() {
        super();
    }

    public WrongThreadException(String detailMessage) {
        super(detailMessage);
    }
}
