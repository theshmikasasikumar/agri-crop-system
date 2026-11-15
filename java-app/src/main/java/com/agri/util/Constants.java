package com.agri.util;

import java.util.List;

/** Allowed values for the coded fields (kept in sync with the CHECK constraints in schema.sql). */
public final class Constants {
    public static final List<String> CATEGORIES =
            List.of("Cereal", "Pulse", "Oilseed", "Cash Crop", "Vegetable", "Millet");
    public static final List<String> SEASONS = List.of("Kharif", "Rabi", "Summer");
    public static final List<String> IRRIGATION_METHODS = List.of("Drip", "Sprinkler", "Flood", "Furrow");
    public static final List<String> QUALITIES = List.of("A", "B", "C");

    private Constants() {
    }
}
