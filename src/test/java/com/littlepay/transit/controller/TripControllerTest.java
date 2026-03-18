package com.littlepay.transit.controller;

import com.littlepay.transit.io.TripWriter;
import com.littlepay.transit.service.TripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TripControllerTest {

    private static final String CSV_RESPONSE =
            "Started, Finished, DurationSecs, FromStopId, ToStopId, ChargeAmount, CompanyId, BusID, PAN, Status\n";

    private MockMvc mockMvc;

    @Mock
    private TripService tripService;

    @Mock
    private TripWriter tripWriter;

    @BeforeEach
    void setUp() {
        StringHttpMessageConverter converter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        converter.setSupportedMediaTypes(List.of(
                MediaType.TEXT_PLAIN,
                MediaType.parseMediaType("text/csv")
        ));

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TripController(tripService, tripWriter))
                .setMessageConverters(converter)
                .build();
    }

    private void stubServiceAndWriter() throws Exception {
        when(tripService.process(any(Reader.class))).thenReturn(List.of());
        doAnswer(inv -> {
            Writer writer = inv.getArgument(1);
            writer.write(CSV_RESPONSE);
            return null;
        }).when(tripWriter).write(anyIterable(), any(Writer.class));
    }

    @Test
    void processTrips_validCsv_returns200WithCsvBody() throws Exception {
        stubServiceAndWriter();
        mockMvc.perform(post("/api/trips")
                        .contentType("text/csv")
                        .accept("text/csv")
                        .content("ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN\n"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(CSV_RESPONSE));
    }

    @Test
    void processTrips_wrongContentType_returns415() throws Exception {
        mockMvc.perform(post("/api/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void processUpload_validFile_returns200WithCsvAttachment() throws Exception {
        stubServiceAndWriter();
        MockMultipartFile file = new MockMultipartFile(
                "file", "taps.csv", "text/csv",
                "ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN\n".getBytes());

        mockMvc.perform(multipart("/api/trips/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("trips.csv")))
                .andExpect(content().string(CSV_RESPONSE));
    }

    @Test
    void processUpload_missingFile_returns400() throws Exception {
        mockMvc.perform(multipart("/api/trips/upload"))
                .andExpect(status().isBadRequest());
    }
}
