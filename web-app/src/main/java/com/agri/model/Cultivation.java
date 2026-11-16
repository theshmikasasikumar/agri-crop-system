package com.agri.model;

import java.time.LocalDate;

/** One crop cultivated on one farm in one season. cultivatedArea is in acres. */
public record Cultivation(int cultivationId, int farmId, int cropId,
                          LocalDate cultivationDate, double cultivatedArea, String season) {
}
