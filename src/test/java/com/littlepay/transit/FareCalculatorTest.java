package com.littlepay.transit;

import com.littlepay.transit.service.FareCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FareCalculatorTest {

    private FareCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = FareCalculator.withDefaultFares();
    }

    @Test
    void fareStop1ToStop2() {
        assertThat(calculator.calculateFare("Stop1", "Stop2"))
                .isEqualByComparingTo("3.25");
    }

    @Test
    void fareStop2ToStop1_isSymmetric() {
        assertThat(calculator.calculateFare("Stop2", "Stop1"))
                .isEqualByComparingTo("3.25");
    }

    @Test
    void fareStop2ToStop3() {
        assertThat(calculator.calculateFare("Stop2", "Stop3"))
                .isEqualByComparingTo("5.50");
    }

    @Test
    void fareStop1ToStop3() {
        assertThat(calculator.calculateFare("Stop1", "Stop3"))
                .isEqualByComparingTo("7.30");
    }

    @Test
    void fareStop3ToStop1_isSymmetric() {
        assertThat(calculator.calculateFare("Stop3", "Stop1"))
                .isEqualByComparingTo("7.30");
    }

    @Test
    void fareSameStop_returnsZero() {
        assertThat(calculator.calculateFare("Stop1", "Stop1"))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void maxFareFromStop1_isStop1ToStop3() {

        assertThat(calculator.calculateMaxFare("Stop1"))
                .isEqualByComparingTo("7.30");
    }

    @Test
    void maxFareFromStop2_isStop2ToStop3() {
        assertThat(calculator.calculateMaxFare("Stop2"))
                .isEqualByComparingTo("5.50");
    }

    @Test
    void maxFareFromStop3_isStop3ToStop1() {
        assertThat(calculator.calculateMaxFare("Stop3"))
                .isEqualByComparingTo("7.30");
    }

    @Test
    void unknownRoute_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> calculator.calculateFare("Stop1", "Stop99"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Stop1")
                .hasMessageContaining("Stop99");
    }
}
