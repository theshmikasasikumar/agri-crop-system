package com.agri.model;

import java.time.LocalDate;

/** Harvest of a cultivation. quantityKg in kg, sellingPrice in INR per kg. */
public record Harvest(int harvestId, int cultivationId, LocalDate harvestDate,
                      double quantityKg, String quality, double sellingPrice) {
}
