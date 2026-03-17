package com.littlepay.transit.io;

import com.littlepay.transit.model.Tap;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

public interface TapReader {
    List<Tap> read(Reader reader) throws IOException;
}
