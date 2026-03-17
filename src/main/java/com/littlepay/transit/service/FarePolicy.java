package com.littlepay.transit.service;

import java.math.BigDecimal;

public interface FarePolicy {
    BigDecimal calculateFare(String fromStop, String toStop);
    BigDecimal calculateMaxFare(String fromStop);
}
