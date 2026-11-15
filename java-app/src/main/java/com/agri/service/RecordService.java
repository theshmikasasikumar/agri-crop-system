package com.agri.service;

import com.agri.dao.CropDao;
import com.agri.dao.CultivationDao;
import com.agri.dao.FarmDao;
import com.agri.dao.FarmerDao;
import com.agri.dao.FertilizerInputDao;
import com.agri.dao.HarvestDao;
import com.agri.dao.IrrigationDao;
import com.agri.model.Crop;
import com.agri.model.Cultivation;
import com.agri.model.Farm;
import com.agri.model.Farmer;
import com.agri.model.FertilizerInput;
import com.agri.model.Harvest;
import com.agri.model.Irrigation;
import com.agri.util.Constants;
import com.agri.util.ValidationException;

import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Business rules for adding records. Every method validates its input, throws a
 * ValidationException with a clear message when a rule is broken, and returns the new id.
 */
public class RecordService {
    private final FarmerDao farmerDao = new FarmerDao();
    private final FarmDao farmDao = new FarmDao();
    private final CropDao cropDao = new CropDao();
    private final CultivationDao cultivationDao = new CultivationDao();
    private final FertilizerInputDao inputDao = new FertilizerInputDao();
    private final IrrigationDao irrigationDao = new IrrigationDao();
    private final HarvestDao harvestDao = new HarvestDao();

    public int addFarmer(String name, String contactNumber, String address) throws SQLException {
        require(!name.isBlank(), "Farmer name is required.");
        require(contactNumber.matches("[6-9]\\d{9}"), "Contact number must be 10 digits starting with 6-9.");
        require(!address.isBlank(), "Address is required.");
        require(!farmerDao.contactExists(contactNumber), "A farmer with this contact number already exists.");
        return farmerDao.add(new Farmer(0, name.trim(), contactNumber, address.trim()));
    }

    public int addFarm(int farmerId, String location, double totalArea) throws SQLException {
        require(farmerDao.exists(farmerId), "Farmer id " + farmerId + " does not exist.");
        require(!location.isBlank(), "Location is required.");
        require(totalArea > 0, "Total area must be greater than 0.");
        return farmDao.add(new Farm(0, farmerId, location.trim(), totalArea));
    }

    public int addCrop(String cropName, String category, int durationDays) throws SQLException {
        require(!cropName.isBlank(), "Crop name is required.");
        require(Constants.CATEGORIES.contains(category), "Category must be one of " + Constants.CATEGORIES + ".");
        require(durationDays > 0, "Duration must be greater than 0 days.");
        require(!cropDao.nameExists(cropName), "Crop '" + cropName + "' already exists.");
        return cropDao.add(new Crop(0, cropName.trim(), category, durationDays));
    }

    public int registerCultivation(int farmId, int cropId, LocalDate date, double area, String season)
            throws SQLException {
        Farm farm = farmDao.findById(farmId)
                .orElseThrow(() -> new ValidationException("Farm id " + farmId + " does not exist."));
        require(cropDao.exists(cropId), "Crop id " + cropId + " does not exist.");
        require(area > 0, "Cultivated area must be greater than 0.");
        require(area <= farm.totalArea(), String.format(
                "Cultivated area (%.2f) cannot exceed the farm's total area (%.2f acres).", area, farm.totalArea()));
        require(Constants.SEASONS.contains(season), "Season must be one of " + Constants.SEASONS + ".");
        return cultivationDao.add(new Cultivation(0, farmId, cropId, date, area, season));
    }

    public int addFertilizerInput(int cultivationId, String type, double quantityKg, double cost, LocalDate date)
            throws SQLException {
        Cultivation c = findCultivation(cultivationId);
        require(!type.isBlank(), "Fertilizer type is required.");
        require(quantityKg > 0, "Quantity must be greater than 0.");
        require(cost >= 0, "Cost cannot be negative.");
        require(!date.isBefore(c.cultivationDate()),
                "Application date cannot be before the cultivation date (" + c.cultivationDate() + ").");
        return inputDao.add(new FertilizerInput(0, cultivationId, type.trim(), quantityKg, cost, date));
    }

    public int addIrrigation(int cultivationId, LocalDate date, double waterLitres, String method)
            throws SQLException {
        Cultivation c = findCultivation(cultivationId);
        require(waterLitres > 0, "Water quantity must be greater than 0.");
        require(Constants.IRRIGATION_METHODS.contains(method),
                "Method must be one of " + Constants.IRRIGATION_METHODS + ".");
        require(!date.isBefore(c.cultivationDate()),
                "Irrigation date cannot be before the cultivation date (" + c.cultivationDate() + ").");
        return irrigationDao.add(new Irrigation(0, cultivationId, date, waterLitres, method));
    }

    public int addHarvest(int cultivationId, LocalDate date, double quantityKg, String quality, double pricePerKg)
            throws SQLException {
        Cultivation c = findCultivation(cultivationId);
        require(harvestDao.findByCultivation(cultivationId).isEmpty(),
                "A harvest is already recorded for cultivation " + cultivationId + ".");
        require(!date.isBefore(c.cultivationDate()),
                "Harvest date cannot be before the cultivation date (" + c.cultivationDate() + ").");
        require(quantityKg > 0, "Quantity harvested must be greater than 0.");
        require(Constants.QUALITIES.contains(quality), "Quality must be one of " + Constants.QUALITIES + ".");
        require(pricePerKg > 0, "Selling price must be greater than 0.");
        return harvestDao.add(new Harvest(0, cultivationId, date, quantityKg, quality, pricePerKg));
    }

    public void updateFarmerContact(int farmerId, String contactNumber) throws SQLException {
        require(contactNumber.matches("[6-9]\\d{9}"), "Contact number must be 10 digits starting with 6-9.");
        require(farmerDao.exists(farmerId), "Farmer id " + farmerId + " does not exist.");
        require(!farmerDao.contactExists(contactNumber), "That contact number is already used by another farmer.");
        farmerDao.updateContact(farmerId, contactNumber);
    }

    public void deleteFarmer(int farmerId) throws SQLException {
        require(farmerDao.delete(farmerId), "Farmer id " + farmerId + " does not exist.");
    }

    // ------------------------------------------------------------------ helpers
    private Cultivation findCultivation(int cultivationId) throws SQLException {
        return cultivationDao.findById(cultivationId)
                .orElseThrow(() -> new ValidationException("Cultivation id " + cultivationId + " does not exist."));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new ValidationException(message);
        }
    }
}
