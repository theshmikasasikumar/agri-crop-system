package com.agri.dao;

import com.agri.model.Cultivation;
import com.agri.util.DatabaseManager;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

/** Database access for the Cultivation table. */
public class CultivationDao {

    public int add(Cultivation c) throws SQLException {
        return DatabaseManager.insert(
                "INSERT INTO Cultivation (farm_id, crop_id, cultivation_date, cultivated_area, season) "
                        + "VALUES (?, ?, ?, ?, ?)",
                c.farmId(), c.cropId(), c.cultivationDate(), c.cultivatedArea(), c.season());
    }

    public Optional<Cultivation> findById(int cultivationId) throws SQLException {
        return DatabaseManager.queryOne(
                "SELECT cultivation_id, farm_id, crop_id, cultivation_date, cultivated_area, season "
                        + "FROM Cultivation WHERE cultivation_id = ?",
                rs -> new Cultivation(rs.getInt("cultivation_id"), rs.getInt("farm_id"), rs.getInt("crop_id"),
                        LocalDate.parse(rs.getString("cultivation_date")),
                        rs.getDouble("cultivated_area"), rs.getString("season")),
                cultivationId);
    }
}
