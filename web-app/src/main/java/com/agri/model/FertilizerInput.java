package com.agri.model;

import java.time.LocalDate;

/** A fertilizer application. quantityKg in kg, inputCost in INR. */
public record FertilizerInput(int inputId, int cultivationId, String fertilizerType,
                              double quantityKg, double inputCost, LocalDate applicationDate) {
}
