package com.littlepay.transit.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public class FareCalculator implements FarePolicy {

    private final Map<Set<String>, BigDecimal> fares;

    public FareCalculator(Map<Set<String>, BigDecimal> fares) {
        this.fares = Map.copyOf(fares);
    }

    public static FareCalculator withDefaultFares() {
        return new FareCalculator(Map.of(
                Set.of("Stop1", "Stop2"), new BigDecimal("3.25"),
                Set.of("Stop2", "Stop3"), new BigDecimal("5.50"),
                Set.of("Stop1", "Stop3"), new BigDecimal("7.30")
        ));
    }

    @Override
    public BigDecimal calculateFare(String fromStop, String toStop) {
        if (fromStop.equals(toStop)) {
            return new BigDecimal("0.00");
        }
        BigDecimal fare = fares.get(Set.of(fromStop, toStop));
        if (fare == null) {
            throw new IllegalArgumentException(
                    "No fare defined for route: " + fromStop + " to " + toStop);
        }
        return fare;
    }

    @Override
    public BigDecimal calculateMaxFare(String fromStop) {
        return fares.entrySet().stream()
                .filter(entry -> entry.getKey().contains(fromStop))
                .map(Map.Entry::getValue)
                .max(BigDecimal::compareTo)
                .orElse(new BigDecimal("0.00"));
    }
}
