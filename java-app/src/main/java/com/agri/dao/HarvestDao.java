package com.agri.dao;

import com.agri.model.Harvest;
import com.agri.util.DatabaseManager;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

/** Database access for the Harvest table. */
public class HarvestDao {

    public int add(Harvest h) throws SQLException {
        return DatabaseManager.insert(
                "INSERT INTO Harvest (cultivation_id, harvest_date, quantity_kg, quality, selling_price) "
                        + "VALUES (?, ?, ?, ?, ?)",
                h.cultivationId(), h.harvestDate(), h.quantityKg(), h.quality(), h.sellingPrice());
    }

    public Optional<Harvest> findByCultivation(int cultivationId) throws SQLException {
        return DatabaseManager.queryOne(
                "SELECT harvest_id, cultivation_id, harvest_date, quantity_kg, quality, selling_price "
                        + "FROM Harvest WHERE cultivation_id = ?",
                rs -> new Harvest(rs.getInt("harvest_id"), rs.getInt("cultivation_id"),
                        LocalDate.parse(rs.getString("harvest_date")), rs.getDouble("quantity_kg"),
                        rs.getString("quality"), rs.getDouble("selling_price")),
                cultivationId);
    }
}
