package com.chandu.roost.dto;

import java.util.List;

public record MonitorPageResponse(
        List<MonitorResponse> content,
        int page,
        int size,
        long totalElements
){}
