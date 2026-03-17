package com.littlepay.transit.model;

import java.time.LocalDateTime;

public record Tap(
        int id,
        LocalDateTime dateTime,
        TapType tapType,
        String stopId,
        String companyId,
        String busId,
        String pan
) {
}