package com.littlepay.transit.service;

import com.littlepay.transit.model.Tap;
import com.littlepay.transit.model.Trip;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TripProcessor {

    public List<Trip> process(List<Tap> taps) {
        List<Tap> sorted = taps.stream()
                .sorted(Comparator.comparing(Tap::dateTime))
                .toList();

        List<Trip> trips = new ArrayList<>();

        return trips;
    }

}
