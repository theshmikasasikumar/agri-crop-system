package com.agri.dao;

import com.agri.model.Irrigation;
import com.agri.util.DatabaseManager;

import java.sql.SQLException;

/** Database access for the Irrigation table. */
public class IrrigationDao {

    public int add(Irrigation i) throws SQLException {
        return DatabaseManager.insert(
                "INSERT INTO Irrigation (cultivation_id, irrigation_date, water_quantity_l, irrigation_method) "
                        + "VALUES (?, ?, ?, ?)",
                i.cultivationId(), i.irrigationDate(), i.waterQuantityLitres(), i.method());
    }

    /** Total litres of water used by one cultivation (0 if none). */
    public double totalWater(int cultivationId) throws SQLException {
        return DatabaseManager.sum("SELECT SUM(water_quantity_l) FROM Irrigation WHERE cultivation_id = ?",
                cultivationId);
    }
}
