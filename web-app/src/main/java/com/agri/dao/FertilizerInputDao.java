package com.agri.dao;

import com.agri.model.FertilizerInput;
import com.agri.util.DatabaseManager;

import java.sql.SQLException;

/** Database access for the FertilizerInput table. */
public class FertilizerInputDao {

    public int add(FertilizerInput f) throws SQLException {
        return DatabaseManager.insert(
                "INSERT INTO FertilizerInput (cultivation_id, fertilizer_type, quantity_kg, input_cost, application_date) "
                        + "VALUES (?, ?, ?, ?, ?)",
                f.cultivationId(), f.fertilizerType(), f.quantityKg(), f.inputCost(), f.applicationDate());
    }

    /** Sum of all fertilizer costs of one cultivation (0 if none). */
    public double totalCost(int cultivationId) throws SQLException {
        return DatabaseManager.sum("SELECT SUM(input_cost) FROM FertilizerInput WHERE cultivation_id = ?",
                cultivationId);
    }
}
