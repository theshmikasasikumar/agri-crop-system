package com.agri.dao;

import com.agri.model.Farmer;
import com.agri.util.DatabaseManager;

import java.sql.SQLException;

/** Database access for the Farmer table. */
public class FarmerDao {

    public int add(Farmer f) throws SQLException {
        return DatabaseManager.insert(
                "INSERT INTO Farmer (name, contact_number, address) VALUES (?, ?, ?)",
                f.name(), f.contactNumber(), f.address());
    }

    public boolean exists(int farmerId) throws SQLException {
        return DatabaseManager.exists("SELECT 1 FROM Farmer WHERE farmer_id = ?", farmerId);
    }

    public boolean contactExists(String contactNumber) throws SQLException {
        return DatabaseManager.exists("SELECT 1 FROM Farmer WHERE contact_number = ?", contactNumber);
    }

    public boolean updateContact(int farmerId, String contactNumber) throws SQLException {
        return DatabaseManager.update("UPDATE Farmer SET contact_number = ? WHERE farmer_id = ?",
                contactNumber, farmerId) > 0;
    }

    /** Deleting a farmer also deletes their farms and everything recorded on them (ON DELETE CASCADE). */
    public boolean delete(int farmerId) throws SQLException {
        return DatabaseManager.update("DELETE FROM Farmer WHERE farmer_id = ?", farmerId) > 0;
    }
}
