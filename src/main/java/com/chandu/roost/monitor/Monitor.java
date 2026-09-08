package com.chandu.roost.monitor;

public record Monitor(
        Long id,
        String name,
        String url,
        String status
) {
}