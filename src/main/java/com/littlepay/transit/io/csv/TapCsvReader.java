package com.littlepay.transit.io.csv;

import com.littlepay.transit.io.TapReader;
import com.littlepay.transit.model.Tap;
import com.littlepay.transit.model.TapType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TapCsvReader implements TapReader {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public List<Tap> read(Reader reader) throws IOException {
        List<Tap> taps = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(reader)) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    taps.add(parseLine(line));
                }
            }
        }
        return taps;
    }

    private Tap parseLine(String line) {
        String[] parts = line.split(",");
        int id = Integer.parseInt(parts[0].trim());
        LocalDateTime dateTime = LocalDateTime.parse(parts[1].trim(), FORMATTER);
        TapType tapType = TapType.valueOf(parts[2].trim());
        String stopId = parts[3].trim();
        String companyId = parts[4].trim();
        String busId = parts[5].trim();
        String pan = parts[6].trim();
        return new Tap(id, dateTime, tapType, stopId, companyId, busId, pan);
    }
}
