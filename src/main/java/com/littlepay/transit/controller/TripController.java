package com.littlepay.transit.controller;

import com.littlepay.transit.io.TripWriter;
import com.littlepay.transit.model.Trip;
import com.littlepay.transit.service.TripService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private static final MediaType TEXT_CSV = MediaType.parseMediaType("text/csv");

    private final TripService tripService;
    private final TripWriter tripWriter;

    public TripController(TripService tripService, TripWriter tripWriter) {
        this.tripService = tripService;
        this.tripWriter = tripWriter;
    }

    @PostMapping(consumes = "text/csv", produces = "text/csv")
    public ResponseEntity<String> processTrips(@RequestBody String csvBody) throws IOException {
        List<Trip> trips = tripService.process(new StringReader(csvBody));

        StringWriter sw = new StringWriter();
        tripWriter.write(trips, sw);

        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .body(sw.toString());
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "text/csv")
    public ResponseEntity<String> processUpload(@RequestParam("file") MultipartFile file) throws IOException {
        List<Trip> trips = tripService.process(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));

        StringWriter sw = new StringWriter();
        tripWriter.write(trips, sw);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("trips.csv").build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(TEXT_CSV)
                .body(sw.toString());
    }
}
