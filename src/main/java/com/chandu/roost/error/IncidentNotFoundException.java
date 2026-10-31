package com.chandu.roost.error;

public class IncidentNotFoundException extends RuntimeException {

    public IncidentNotFoundException(Long id) {
        super("Incident not found: " + id);
    }
}
