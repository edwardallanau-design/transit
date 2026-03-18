package com.littlepay.transit.service;

import com.littlepay.transit.io.TapReader;
import com.littlepay.transit.model.Tap;
import com.littlepay.transit.model.Trip;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TapReader tapReader;

    @Mock
    private TripProcessor tripProcessor;

    @InjectMocks
    private TripService tripService;

    @Test
    void process_delegatesReadThenProcess() throws IOException {
        Reader reader = new StringReader("");
        List<Tap> taps = List.of();
        List<Trip> expectedTrips = List.of();

        when(tapReader.read(reader)).thenReturn(taps);
        when(tripProcessor.process(taps)).thenReturn(expectedTrips);

        List<Trip> result = tripService.process(reader);

        assertThat(result).isSameAs(expectedTrips);
        verify(tapReader).read(reader);
        verify(tripProcessor).process(taps);
    }
}