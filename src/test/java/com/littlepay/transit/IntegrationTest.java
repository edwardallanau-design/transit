package com.littlepay.transit;

import com.littlepay.transit.io.csv.TapCsvReader;
import com.littlepay.transit.io.csv.TripCsvWriter;
import com.littlepay.transit.model.Tap;
import com.littlepay.transit.model.Trip;
import com.littlepay.transit.service.FareCalculator;
import com.littlepay.transit.service.TripProcessor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationTest {

    private static final String EXAMPLE_CSV = """
            ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN
            1, 22-01-2023 13:00:00, ON, Stop1, Company1, Bus37, 5500005555555559
            2, 22-01-2023 13:05:00, OFF, Stop2, Company1, Bus37, 5500005555555559
            3, 22-01-2023 09:20:00, ON, Stop3, Company1, Bus36, 4111111111111111
            4, 23-01-2023 08:00:00, ON, Stop1, Company1, Bus37, 4111111111111111
            5, 23-01-2023 08:02:00, OFF, Stop1, Company1, Bus37, 4111111111111111
            6, 24-01-2023 16:30:00, OFF, Stop2, Company1, Bus37, 5500005555555559
            """;

    @Test
    void endToEnd_exampleInput_parsesAllSixTaps() throws IOException {
        TapCsvReader reader = new TapCsvReader();
        List<Tap> taps = reader.read(new StringReader(EXAMPLE_CSV));
        assertThat(taps).hasSize(6);
    }

    @Test
    void endToEnd_exampleInput_producesThreeTrips() throws IOException {
        List<Trip> trips = processExample();

        assertThat(trips).hasSize(3);
    }

    @Test
    void endToEnd_outputContainsAllStatuses() throws IOException {
        String output = writeToString(processExample());

        assertThat(output).contains("COMPLETED");
        assertThat(output).contains("INCOMPLETE");
        assertThat(output).contains("CANCELLED");
    }

    @Test
    void endToEnd_outputContainsCorrectFares() throws IOException {
        String output = writeToString(processExample());

        assertThat(output).contains("$3.25");
        assertThat(output).contains("$7.30");
        assertThat(output).contains("$0.00");
    }

    @Test
    void endToEnd_outputContainsHeader() throws IOException {
        String output = writeToString(processExample());

        assertThat(output).startsWith("Started, Finished, DurationSecs");
    }

    @Test
    void endToEnd_completedTrip_hasCorrectDuration() throws IOException {
        List<Trip> trips = processExample();

        Trip completed = trips.stream()
                .filter(t -> t.status().name().equals("COMPLETED"))
                .findFirst().orElseThrow();

        assertThat(completed.durationSecs()).isEqualTo(300L);
    }

    @Test
    void endToEnd_incompleteTrip_hasNullFinished() throws IOException {
        List<Trip> trips = processExample();

        Trip incomplete = trips.stream()
                .filter(t -> t.status().name().equals("INCOMPLETE"))
                .findFirst().orElseThrow();

        assertThat(incomplete.finished()).isNull();
        assertThat(incomplete.toStopId()).isNull();
        assertThat(incomplete.durationSecs()).isEqualTo(0L);
    }

    private List<Trip> processExample() throws IOException {
        TapCsvReader reader = new TapCsvReader();
        TripProcessor processor = new TripProcessor(FareCalculator.withDefaultFares());
        List<Tap> taps = reader.read(new StringReader(EXAMPLE_CSV));
        return processor.process(taps);
    }

    private String writeToString(List<Trip> trips) throws IOException {
        TripCsvWriter writer = new TripCsvWriter();
        StringWriter sw = new StringWriter();
        writer.write(trips, sw);
        return sw.toString();
    }
}
