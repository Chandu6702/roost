package com.chandu.roost.controller;

import com.chandu.roost.dto.ProbeResultRequest;
import com.chandu.roost.dto.ProbeResultResponse;
import com.chandu.roost.service.ProbeResultService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/monitors/{monitorId}/probe-results")
public class ProbeResultController {

    private final ProbeResultService probeResultService;

    public ProbeResultController(ProbeResultService probeResultService) {
        this.probeResultService = probeResultService;
    }

    @PostMapping
    public ResponseEntity<ProbeResultResponse> createProbeResult(
            @PathVariable Long monitorId,
            @Valid @RequestBody ProbeResultRequest request) {

        ProbeResultResponse response =
                probeResultService.create(monitorId, request);

        return ResponseEntity
                .status(201)
                .body(response);
    }
}