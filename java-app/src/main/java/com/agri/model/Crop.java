package com.agri.model;

/** A crop that can be cultivated. */
public record Crop(int cropId, String cropName, String category, int durationDays) {
}
