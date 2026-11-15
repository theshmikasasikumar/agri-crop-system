package com.agri.dao;

import com.agri.model.Crop;
import com.agri.util.DatabaseManager;

import java.sql.SQLException;
import java.util.Optional;

/** Database access for the Crop table. */
public class CropDao {

    public int add(Crop c) throws SQLException {
        return DatabaseManager.insert(
                "INSERT INTO Crop (crop_name, crop_category, duration_days) VALUES (?, ?, ?)",
                c.cropName(), c.category(), c.durationDays());
    }

    public boolean exists(int cropId) throws SQLException {
        return DatabaseManager.exists("SELECT 1 FROM Crop WHERE crop_id = ?", cropId);
    }

    public boolean nameExists(String cropName) throws SQLException {
        return DatabaseManager.exists("SELECT 1 FROM Crop WHERE lower(crop_name) = lower(?)", cropName);
    }

    public Optional<String> findNameById(int cropId) throws SQLException {
        return DatabaseManager.queryOne("SELECT crop_name FROM Crop WHERE crop_id = ?",
                rs -> rs.getString("crop_name"), cropId);
    }
}
