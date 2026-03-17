package com.littlepay.transit.io;

import com.littlepay.transit.model.Trip;

import java.io.IOException;
import java.io.Writer;

public interface TripWriter {
    void write(Iterable<Trip> trips, Writer writer) throws IOException;
}
