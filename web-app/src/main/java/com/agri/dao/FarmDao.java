package com.agri.dao;

import com.agri.model.Farm;
import com.agri.util.DatabaseManager;

import java.sql.SQLException;
import java.util.Optional;

/** Database access for the Farm table. */
public class FarmDao {

    public int add(Farm f) throws SQLException {
        return DatabaseManager.insert(
                "INSERT INTO Farm (farmer_id, location, total_area) VALUES (?, ?, ?)",
                f.farmerId(), f.location(), f.totalArea());
    }

    public Optional<Farm> findById(int farmId) throws SQLException {
        return DatabaseManager.queryOne(
                "SELECT farm_id, farmer_id, location, total_area FROM Farm WHERE farm_id = ?",
                rs -> new Farm(rs.getInt("farm_id"), rs.getInt("farmer_id"),
                        rs.getString("location"), rs.getDouble("total_area")),
                farmId);
    }
}
