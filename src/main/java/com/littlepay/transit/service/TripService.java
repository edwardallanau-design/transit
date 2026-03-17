package com.littlepay.transit.service;

import com.littlepay.transit.io.TapReader;
import com.littlepay.transit.model.Tap;
import com.littlepay.transit.model.Trip;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

public class TripService {

    private final TapReader tapReader;
    private final TripProcessor tripProcessor;

    public TripService(TapReader tapReader, TripProcessor tripProcessor) {
        this.tapReader = tapReader;
        this.tripProcessor = tripProcessor;
    }

    public List<Trip> process(Reader reader) throws IOException {
        List<Tap> taps = tapReader.read(reader);
        return tripProcessor.process(taps);
    }
}
