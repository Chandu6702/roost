package com.chandu.roost.incident;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<IncidentResponse>> listIncidents(
            Pageable pageable,
            Authentication authentication) {

        return ResponseEntity.ok(
                incidentService.listForUser(authentication.getName(), pageable));
    }

    @PostMapping("/monitors/{monitorId}/incidents/{incidentId}/acknowledge")
    public ResponseEntity<Void> acknowledge(
            @PathVariable Long monitorId,
            @PathVariable Long incidentId) {

        incidentService.acknowledge(incidentId);

        return ResponseEntity.noContent().build();
    }


}
