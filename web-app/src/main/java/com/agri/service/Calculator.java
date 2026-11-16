package com.agri.service;

/** Pure business formulas (no database access, so they are easy to unit-test). */
public final class Calculator {

    private Calculator() {
    }

    /** Yield per unit area = quantity harvested (kg) / cultivated area (acres). */
    public static double yieldPerArea(double quantityKg, double areaAcres) {
        if (areaAcres <= 0) {
            throw new IllegalArgumentException("Area must be greater than 0");
        }
        return round2(quantityKg / areaAcres);
    }

    /** Sales revenue = quantity harvested (kg) x selling price (INR per kg). */
    public static double revenue(double quantityKg, double pricePerKg) {
        return round2(quantityKg * pricePerKg);
    }

    /** Estimated profit = revenue - total input cost. May be negative (a loss). */
    public static double profit(double revenue, double totalInputCost) {
        return round2(revenue - totalInputCost);
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
