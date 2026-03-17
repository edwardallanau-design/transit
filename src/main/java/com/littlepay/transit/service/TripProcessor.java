package com.littlepay.transit.service;

import com.littlepay.transit.model.Tap;
import com.littlepay.transit.model.TapType;
import com.littlepay.transit.model.Trip;
import com.littlepay.transit.model.TripStatus;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TripProcessor {

    private final FarePolicy farePolicy;

    public TripProcessor(FarePolicy farePolicy) {
        this.farePolicy = farePolicy;
    }

    public List<Trip> process(List<Tap> taps) {
        List<Tap> sorted = taps.stream()
                .sorted(Comparator.comparing(Tap::dateTime))
                .toList();

        List<Trip> trips = new ArrayList<>();
        Map<String, Tap> activeTapOns = new LinkedHashMap<>(); // PAN → active tap-ON

        for (Tap tap : sorted) {
            if (tap.tapType() == TapType.ON) {
                handleTapOn(tap, activeTapOns, trips);
            } else {
                handleTapOff(tap, activeTapOns, trips);
            }
        }

        activeTapOns.values().forEach(tapOn -> trips.add(buildIncompleteTrip(tapOn)));
        trips.sort(Comparator.comparing(Trip::started));
        return trips;
    }

    private void handleTapOn(Tap tap, Map<String, Tap> activeTapOns, List<Trip> trips) {
        Tap previous = activeTapOns.put(tap.pan(), tap);
        if (previous != null) {
            trips.add(buildIncompleteTrip(previous));
        }
    }

    private void handleTapOff(Tap tap, Map<String, Tap> activeTapOns, List<Trip> trips) {
        Tap tapOn = activeTapOns.remove(tap.pan());
        if (tapOn == null) {
            return;
        }
        trips.add(buildCompletedOrCancelledTrip(tapOn, tap));
    }

    private Trip buildCompletedOrCancelledTrip(Tap tapOn, Tap tapOff) {
        boolean cancelled = tapOn.stopId().equals(tapOff.stopId());
        TripStatus status = cancelled ? TripStatus.CANCELLED : TripStatus.COMPLETED;
        BigDecimal charge = farePolicy.calculateFare(tapOn.stopId(), tapOff.stopId());
        long duration = ChronoUnit.SECONDS.between(tapOn.dateTime(), tapOff.dateTime());

        return new Trip(
                tapOn.dateTime(),
                tapOff.dateTime(),
                duration,
                tapOn.stopId(),
                tapOff.stopId(),
                charge,
                tapOn.companyId(),
                tapOn.busId(),
                tapOn.pan(),
                status
        );
    }

    private Trip buildIncompleteTrip(Tap tapOn) {
        BigDecimal charge = farePolicy.calculateMaxFare(tapOn.stopId());

        return new Trip(
                tapOn.dateTime(),
                null,
                0L,
                tapOn.stopId(),
                null,
                charge,
                tapOn.companyId(),
                tapOn.busId(),
                tapOn.pan(),
                TripStatus.INCOMPLETE
        );
    }
}
