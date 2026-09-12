package com.chandu.roost.monitor;

import java.util.List;

public record MonitorPageResponse(
        List<MonitorResponse> content,
        int page,
        int size,
        long totalElements
){}
