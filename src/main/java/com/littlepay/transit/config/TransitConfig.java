package com.littlepay.transit.config;

import com.littlepay.transit.io.TapReader;
import com.littlepay.transit.io.TripWriter;
import com.littlepay.transit.io.csv.TapCsvReader;
import com.littlepay.transit.io.csv.TripCsvWriter;
import com.littlepay.transit.service.FareCalculator;
import com.littlepay.transit.service.FarePolicy;
import com.littlepay.transit.service.TripProcessor;
import com.littlepay.transit.service.TripService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class TransitConfig {

    @Bean
    public FareCalculator fareCalculator() {
        return FareCalculator.withDefaultFares();
    }

    @Bean
    public TripProcessor tripProcessor(FarePolicy farePolicy) {
        return new TripProcessor(farePolicy);
    }

    @Bean
    public TapReader tapReader() {
        return new TapCsvReader();
    }

    @Bean
    public TripWriter tripWriter() {
        return new TripCsvWriter();
    }

    @Bean
    public TripService tripService(TapReader tapReader, TripProcessor tripProcessor) {
        return new TripService(tapReader, tripProcessor);
    }

    @Bean
    public StringHttpMessageConverter stringHttpMessageConverter() {
        StringHttpMessageConverter converter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        List<MediaType> mediaTypes = List.of(
                MediaType.TEXT_PLAIN,
                MediaType.TEXT_HTML,
                MediaType.parseMediaType("text/csv;charset=UTF-8"),
                MediaType.ALL
        );
        converter.setSupportedMediaTypes(mediaTypes);
        return converter;
    }
}
