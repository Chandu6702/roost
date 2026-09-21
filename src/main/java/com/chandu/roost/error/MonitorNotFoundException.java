package com.chandu.roost.error;

public class MonitorNotFoundException extends RuntimeException {

    public MonitorNotFoundException(Long id) {
        super("Monitor with id " + id + " was not found");
    }
}