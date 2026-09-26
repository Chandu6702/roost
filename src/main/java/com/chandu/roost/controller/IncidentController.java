package com.chandu.roost.controller;

import com.chandu.roost.service.IncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping("/monitors/{monitorId}/incidents/{incidentId}/acknowledge")
    public ResponseEntity<Void> acknowledge(
            @PathVariable Long monitorId,
            @PathVariable Long incidentId) {

        incidentService.acknowledge(incidentId);

        return ResponseEntity.noContent().build();
    }


}