package com.littlepay.transit.io.csv;

import com.littlepay.transit.io.TripWriter;
import com.littlepay.transit.model.Trip;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.time.format.DateTimeFormatter;

public class TripCsvWriter implements TripWriter {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private static final String HEADER =
            "Started, Finished, DurationSecs, FromStopId, ToStopId, ChargeAmount, CompanyId, BusID, PAN, Status";

    public void write(Iterable<Trip> trips, Writer writer) throws IOException {
        try (PrintWriter pw = new PrintWriter(writer)) {
            pw.println(HEADER);
            for (Trip trip : trips) {
                pw.println(format(trip));
            }
        }
    }

    private String format(Trip trip) {
        String finished = trip.finished() != null ? FORMATTER.format(trip.finished()) : "";
        String toStop = trip.toStopId() != null ? trip.toStopId() : "";
        String charge = String.format("$%.2f", trip.chargeAmount());

        return String.join(", ",
                FORMATTER.format(trip.started()),
                finished,
                String.valueOf(trip.durationSecs()),
                trip.fromStopId(),
                toStop,
                charge,
                trip.companyId(),
                trip.busId(),
                trip.pan(),
                trip.status().name()
        );
    }
}
