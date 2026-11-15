package com.agri.service;

import com.agri.dao.CropDao;
import com.agri.dao.CultivationDao;
import com.agri.dao.FertilizerInputDao;
import com.agri.dao.HarvestDao;
import com.agri.dao.IrrigationDao;
import com.agri.model.Cultivation;
import com.agri.model.Harvest;
import com.agri.util.TablePrinter;
import com.agri.util.ValidationException;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

/** Productivity / cost calculations and the text reports. */
public class ReportService {
    private final CultivationDao cultivationDao = new CultivationDao();
    private final CropDao cropDao = new CropDao();
    private final FertilizerInputDao inputDao = new FertilizerInputDao();
    private final IrrigationDao irrigationDao = new IrrigationDao();
    private final HarvestDao harvestDao = new HarvestDao();

    /** Table name shown in the menu -> real table name (whitelist, so no user text reaches the SQL). */
    private static final Map<Integer, String[]> TABLES = Map.of(
            1, new String[]{"Farmers", "Farmer"},
            2, new String[]{"Farms", "Farm"},
            3, new String[]{"Crops", "Crop"},
            4, new String[]{"Cultivations", "Cultivation"},
            5, new String[]{"Fertilizer inputs", "FertilizerInput"},
            6, new String[]{"Irrigation records", "Irrigation"},
            7, new String[]{"Harvests", "Harvest"});

    // ------------------------------------------------------------ calculation
    public ProductivityReport calculate(int cultivationId) throws SQLException {
        Cultivation c = cultivationDao.findById(cultivationId)
                .orElseThrow(() -> new ValidationException("Cultivation id " + cultivationId + " does not exist."));
        String cropName = cropDao.findNameById(c.cropId()).orElse("?");
        double inputCost = Calculator.round2(inputDao.totalCost(cultivationId));
        double water = irrigationDao.totalWater(cultivationId);

        Optional<Harvest> harvest = harvestDao.findByCultivation(cultivationId);
        if (harvest.isEmpty()) {
            return new ProductivityReport(cultivationId, cropName, c.season(), c.cultivatedArea(),
                    inputCost, water, false, 0, 0, 0, 0);
        }
        Harvest h = harvest.get();
        double yield = Calculator.yieldPerArea(h.quantityKg(), c.cultivatedArea());
        double revenue = Calculator.revenue(h.quantityKg(), h.sellingPrice());
        double profit = Calculator.profit(revenue, inputCost);
        return new ProductivityReport(cultivationId, cropName, c.season(), c.cultivatedArea(),
                inputCost, water, true, h.quantityKg(), yield, revenue, profit);
    }

    // ---------------------------------------------------------------- listing
    public void listTable(int menuNumber) throws SQLException {
        String[] table = TABLES.get(menuNumber);
        if (table == null) {
            throw new ValidationException("Unknown table choice.");
        }
        TablePrinter.print(table[0], "SELECT * FROM " + table[1]);
    }

    // ---------------------------------------------------------------- reports
    public void cropWiseYield() throws SQLException {
        TablePrinter.print("Crop-wise yield (harvested cultivations)",
                "SELECT crop_name AS crop, COUNT(*) AS harvests, ROUND(SUM(quantity_kg), 1) AS total_kg, "
                        + "ROUND(SUM(quantity_kg) / SUM(cultivated_area), 2) AS yield_kg_per_acre "
                        + "FROM v_cultivation_summary WHERE quantity_kg IS NOT NULL "
                        + "GROUP BY crop_name ORDER BY yield_kg_per_acre DESC");
    }

    public void farmWiseProduction() throws SQLException {
        TablePrinter.print("Farm-wise production",
                "SELECT farm_id, location, farmer_name AS farmer, COUNT(*) AS harvests, "
                        + "ROUND(SUM(quantity_kg), 1) AS total_production_kg, ROUND(SUM(revenue), 2) AS total_revenue "
                        + "FROM v_cultivation_summary WHERE quantity_kg IS NOT NULL "
                        + "GROUP BY farm_id, location, farmer_name ORDER BY total_production_kg DESC");
    }

    public void inputCosts() throws SQLException {
        TablePrinter.print("Agricultural input costs by crop",
                "SELECT cr.crop_name AS crop, ROUND(SUM(fi.quantity_kg), 1) AS fertilizer_kg, "
                        + "ROUND(SUM(fi.input_cost), 2) AS total_input_cost "
                        + "FROM FertilizerInput fi "
                        + "JOIN Cultivation c ON c.cultivation_id = fi.cultivation_id "
                        + "JOIN Crop cr ON cr.crop_id = c.crop_id "
                        + "GROUP BY cr.crop_name ORDER BY total_input_cost DESC");
    }

    public void seasonalSummary() throws SQLException {
        TablePrinter.print("Seasonal harvest summary",
                "SELECT season, COUNT(*) AS harvests, ROUND(SUM(quantity_kg), 1) AS total_harvest_kg, "
                        + "ROUND(SUM(revenue), 2) AS revenue, ROUND(SUM(total_input_cost), 2) AS input_cost, "
                        + "ROUND(SUM(profit), 2) AS profit "
                        + "FROM v_cultivation_summary WHERE quantity_kg IS NOT NULL "
                        + "GROUP BY season ORDER BY season");
    }
}
