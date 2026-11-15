package com.agri.model;

import java.time.LocalDate;

/** An irrigation event. waterQuantityLitres is in litres. */
public record Irrigation(int irrigationId, int cultivationId, LocalDate irrigationDate,
                         double waterQuantityLitres, String method) {
}
