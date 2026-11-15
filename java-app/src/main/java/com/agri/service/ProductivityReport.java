package com.agri.service;

/**
 * Result of the productivity and cost calculation for one cultivation.
 * When harvested is false the harvest based figures (yield, revenue, profit) are 0.
 */
public record ProductivityReport(int cultivationId, String cropName, String season, double areaAcres,
                                 double totalInputCost, double totalWaterLitres, boolean harvested,
                                 double quantityKg, double yieldPerAcre, double revenue, double profit) {

    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%nProductivity report for cultivation #%d%n", cultivationId));
        sb.append("--------------------------------------------\n");
        sb.append(String.format("Crop / season        : %s / %s%n", cropName, season));
        sb.append(String.format("Cultivated area      : %,.2f acres%n", areaAcres));
        sb.append(String.format("Total input cost     : INR %,.2f%n", totalInputCost));
        sb.append(String.format("Total water used     : %,.0f litres%n", totalWaterLitres));
        if (!harvested) {
            sb.append("Harvest              : not recorded yet (yield, revenue and profit unavailable)\n");
        } else {
            sb.append(String.format("Quantity harvested   : %,.2f kg%n", quantityKg));
            sb.append(String.format("Yield per acre       : %,.2f kg/acre%n", yieldPerAcre));
            sb.append(String.format("Sales revenue        : INR %,.2f%n", revenue));
            sb.append(String.format("Estimated profit     : INR %,.2f%n", profit));
        }
        return sb.toString();
    }
}
