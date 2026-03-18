package com.littlepay.transit;

import com.littlepay.transit.model.Tap;
import com.littlepay.transit.model.TapType;
import com.littlepay.transit.model.Trip;
import com.littlepay.transit.model.TripStatus;
import com.littlepay.transit.service.FareCalculator;
import com.littlepay.transit.service.TripProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TripProcessorTest {

    private static final String PAN_A = "5500005555555559";
    private static final String PAN_B = "4111111111111111";
    private static final LocalDateTime BASE = LocalDateTime.of(2023, 1, 22, 13, 0, 0);

    private TripProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TripProcessor(FareCalculator.withDefaultFares());
    }

    private Tap tap(int id, LocalDateTime dt, TapType type, String stop, String pan) {
        return new Tap(id, dt, type, stop, "Company1", "Bus37", pan);
    }

    @Test
    void completedTrip_chargesCorrectFare() {
        List<Trip> trips = processor.process(List.of(
                tap(1, BASE, TapType.ON, "Stop1", PAN_A),
                tap(2, BASE.plusMinutes(5), TapType.OFF, "Stop2", PAN_A)
        ));

        assertThat(trips).hasSize(1);
        Trip trip = trips.get(0);
        assertThat(trip.status()).isEqualTo(TripStatus.COMPLETED);
        assertThat(trip.fromStopId()).isEqualTo("Stop1");
        assertThat(trip.toStopId()).isEqualTo("Stop2");
        assertThat(trip.chargeAmount()).isEqualByComparingTo("3.25");
        assertThat(trip.durationSecs()).isEqualTo(300L);
        assertThat(trip.finished()).isNotNull();
    }

    @Test
    void cancelledTrip_sameStop_chargesZero() {
        List<Trip> trips = processor.process(List.of(
                tap(1, BASE, TapType.ON, "Stop1", PAN_A),
                tap(2, BASE.plusMinutes(1), TapType.OFF, "Stop1", PAN_A)
        ));

        assertThat(trips).hasSize(1);
        Trip trip = trips.get(0);
        assertThat(trip.status()).isEqualTo(TripStatus.CANCELLED);
        assertThat(trip.chargeAmount()).isEqualByComparingTo("0.00");
        assertThat(trip.durationSecs()).isEqualTo(60L);
    }

    @Test
    void incompleteTrip_chargesMaxFare() {
        List<Trip> trips = processor.process(List.of(
                tap(1, BASE, TapType.ON, "Stop2", PAN_A)
        ));

        assertThat(trips).hasSize(1);
        Trip trip = trips.get(0);
        assertThat(trip.status()).isEqualTo(TripStatus.INCOMPLETE);
        assertThat(trip.chargeAmount()).isEqualByComparingTo("5.50");
        assertThat(trip.finished()).isNull();
        assertThat(trip.toStopId()).isNull();
        assertThat(trip.durationSecs()).isEqualTo(0L);
    }

    @Test
    void incompleteTrip_fromStop1_chargesMaxFare() {
        List<Trip> trips = processor.process(List.of(
                tap(1, BASE, TapType.ON, "Stop1", PAN_A)
        ));

        assertThat(trips).hasSize(1);
        assertThat(trips.get(0).chargeAmount()).isEqualByComparingTo("7.30");
    }

    @Test
    void orphanedTapOff_isIgnored() {
        List<Trip> trips = processor.process(List.of(
                tap(1, BASE, TapType.OFF, "Stop2", PAN_A)
        ));

        assertThat(trips).isEmpty();
    }

    @Test
    void multiplePans_processedIndependently() {
        List<Trip> trips = processor.process(List.of(
                tap(1, BASE, TapType.ON, "Stop1", PAN_A),
                tap(2, BASE.plusMinutes(1), TapType.ON, "Stop3", PAN_B),
                tap(3, BASE.plusMinutes(5), TapType.OFF, "Stop2", PAN_A)
        ));

        assertThat(trips).hasSize(2);

        Trip panATrip = trips.stream().filter(t -> t.pan().equals(PAN_A)).findFirst().orElseThrow();
        assertThat(panATrip.status()).isEqualTo(TripStatus.COMPLETED);
        assertThat(panATrip.chargeAmount()).isEqualByComparingTo("3.25");

        Trip panBTrip = trips.stream().filter(t -> t.pan().equals(PAN_B)).findFirst().orElseThrow();
        assertThat(panBTrip.status()).isEqualTo(TripStatus.INCOMPLETE);
        assertThat(panBTrip.chargeAmount()).isEqualByComparingTo("7.30"); // max from Stop3
    }

    @Test
    void consecutiveTapOns_firstBecomesIncomplete() {
        List<Trip> trips = processor.process(List.of(
                tap(1, BASE, TapType.ON, "Stop1", PAN_A),
                tap(2, BASE.plusMinutes(10), TapType.ON, "Stop2", PAN_A),
                tap(3, BASE.plusMinutes(15), TapType.OFF, "Stop3", PAN_A)
        ));

        assertThat(trips).hasSize(2);

        Trip incomplete = trips.stream()
                .filter(t -> t.status() == TripStatus.INCOMPLETE).findFirst().orElseThrow();
        assertThat(incomplete.fromStopId()).isEqualTo("Stop1");
        assertThat(incomplete.chargeAmount()).isEqualByComparingTo("7.30");

        Trip completed = trips.stream()
                .filter(t -> t.status() == TripStatus.COMPLETED).findFirst().orElseThrow();
        assertThat(completed.fromStopId()).isEqualTo("Stop2");
        assertThat(completed.toStopId()).isEqualTo("Stop3");
        assertThat(completed.chargeAmount()).isEqualByComparingTo("5.50");
    }

    @Test
    void outOfOrderTaps_sortedBeforeProcessing() {
        List<Trip> trips = processor.process(List.of(
                tap(2, BASE.plusMinutes(5), TapType.OFF, "Stop2", PAN_A),
                tap(1, BASE, TapType.ON, "Stop1", PAN_A)
        ));

        assertThat(trips).hasSize(1);
        assertThat(trips.get(0).status()).isEqualTo(TripStatus.COMPLETED);
        assertThat(trips.get(0).chargeAmount()).isEqualByComparingTo("3.25");
    }

    @Test
    void fullExampleFromSpec_producesThreeTrips() {
        List<Tap> taps = List.of(
                new Tap(1, LocalDateTime.of(2023, 1, 22, 13, 0, 0), TapType.ON, "Stop1", "Company1", "Bus37", PAN_A),
                new Tap(2, LocalDateTime.of(2023, 1, 22, 13, 5, 0), TapType.OFF, "Stop2", "Company1", "Bus37", PAN_A),
                new Tap(3, LocalDateTime.of(2023, 1, 22, 9, 20, 0), TapType.ON, "Stop3", "Company1", "Bus36", PAN_B),
                new Tap(4, LocalDateTime.of(2023, 1, 23, 8, 0, 0), TapType.ON, "Stop1", "Company1", "Bus37", PAN_B),
                new Tap(5, LocalDateTime.of(2023, 1, 23, 8, 2, 0), TapType.OFF, "Stop1", "Company1", "Bus37", PAN_B),
                new Tap(6, LocalDateTime.of(2023, 1, 24, 16, 30, 0), TapType.OFF, "Stop2", "Company1", "Bus37", PAN_A)
        );

        List<Trip> trips = processor.process(taps);

        assertThat(trips).hasSize(3);

        assertThat(trips).anySatisfy(t -> {
            assertThat(t.status()).isEqualTo(TripStatus.INCOMPLETE);
            assertThat(t.fromStopId()).isEqualTo("Stop3");
            assertThat(t.chargeAmount()).isEqualByComparingTo("7.30");
        });

        assertThat(trips).anySatisfy(t -> {
            assertThat(t.status()).isEqualTo(TripStatus.COMPLETED);
            assertThat(t.fromStopId()).isEqualTo("Stop1");
            assertThat(t.toStopId()).isEqualTo("Stop2");
            assertThat(t.chargeAmount()).isEqualByComparingTo("3.25");
        });

        assertThat(trips).anySatisfy(t -> {
            assertThat(t.status()).isEqualTo(TripStatus.CANCELLED);
            assertThat(t.fromStopId()).isEqualTo("Stop1");
            assertThat(t.chargeAmount()).isEqualByComparingTo("0.00");
        });
    }
}
