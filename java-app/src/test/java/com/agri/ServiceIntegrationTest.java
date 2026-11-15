package com.agri;

import com.agri.service.ProductivityReport;
import com.agri.service.RecordService;
import com.agri.service.ReportService;
import com.agri.util.DatabaseManager;
import com.agri.util.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the services against a throw-away SQLite file built from database/schema.sql and
 * database/sample_data.sql (Maven runs tests from the java-app folder, so the scripts are in ../database).
 */
class ServiceIntegrationTest {
    private final RecordService records = new RecordService();
    private final ReportService reports = new ReportService();

    @BeforeEach
    void createDatabase(@TempDir Path tempDir) throws Exception {
        System.setProperty("agri.db", tempDir.resolve("test.db").toString());
        System.setProperty("agri.dbdir", "../database");
        DatabaseManager.initializeSchema();
        DatabaseManager.loadSampleData();
    }

    @AfterEach
    void cleanUp() {
        System.clearProperty("agri.db");
        System.clearProperty("agri.dbdir");
    }

    @Test
    void sampleDataIsLoaded() throws Exception {
        assertTrue(DatabaseManager.exists("SELECT 1 FROM Farmer WHERE farmer_id = 8"));
        assertFalse(DatabaseManager.exists("SELECT 1 FROM Farmer WHERE farmer_id = 9"));
    }

    @Test
    void productivityOfHarvestedCultivation() throws Exception {
        ProductivityReport r = reports.calculate(1);          // Rice, Kharif 2024, 8 acres
        assertEquals("Rice", r.cropName());
        assertTrue(r.harvested());
        assertEquals(1801.08, r.yieldPerAcre(), 0.02);
        assertEquals(12744.69, r.totalInputCost(), 0.02);
        assertEquals(280967.70, r.revenue(), 0.02);
        assertEquals(268223.01, r.profit(), 0.05);
    }

    @Test
    void productivityOfCultivationWithoutHarvest() throws Exception {
        ProductivityReport r = reports.calculate(26);         // ongoing Rabi 2026 cultivation
        assertFalse(r.harvested());
        assertEquals(0.0, r.revenue(), 0.0001);
    }

    @Test
    void cultivatedAreaCannotExceedFarmArea() {
        // farm 1 has 12 acres
        assertThrows(ValidationException.class,
                () -> records.registerCultivation(1, 1, LocalDate.of(2026, 7, 1), 13.0, "Kharif"));
    }

    @Test
    void unknownFarmIsRejected() {
        assertThrows(ValidationException.class,
                () -> records.registerCultivation(999, 1, LocalDate.of(2026, 7, 1), 2.0, "Kharif"));
    }

    @Test
    void secondHarvestForSameCultivationIsRejected() {
        assertThrows(ValidationException.class,
                () -> records.addHarvest(1, LocalDate.of(2025, 1, 1), 100, "A", 20));
    }

    @Test
    void duplicateContactNumberIsRejected() {
        assertThrows(ValidationException.class, () -> records.addFarmer("Test Farmer", "9843012345", "Somewhere"));
    }

    @Test
    void inputBeforeCultivationDateIsRejected() {
        // cultivation 1 starts on 2024-07-10
        assertThrows(ValidationException.class,
                () -> records.addFertilizerInput(1, "Urea", 50, 400, LocalDate.of(2024, 7, 1)));
    }

    @Test
    void validRecordsAreSavedAndGetNewIds() throws Exception {
        int farmerId = records.addFarmer("Test Farmer", "9000000001", "Test Address");
        assertEquals(9, farmerId);
        int farmId = records.addFarm(farmerId, "Test Location", 5.0);
        int cultivationId = records.registerCultivation(farmId, 1, LocalDate.of(2026, 7, 1), 4.0, "Kharif");
        records.addFertilizerInput(cultivationId, "Urea", 100, 650, LocalDate.of(2026, 7, 10));
        records.addIrrigation(cultivationId, LocalDate.of(2026, 7, 12), 40000, "Drip");
        records.addHarvest(cultivationId, LocalDate.of(2026, 11, 5), 8000, "A", 24);

        ProductivityReport r = reports.calculate(cultivationId);
        assertEquals(2000.0, r.yieldPerAcre(), 0.001);          // 8000 kg / 4 acres
        assertEquals(192000.0, r.revenue(), 0.001);              // 8000 x 24
        assertEquals(191350.0, r.profit(), 0.001);               // 192000 - 650
    }

    @Test
    void deletingFarmerRemovesTheirFarms() throws Exception {
        records.deleteFarmer(1);
        assertFalse(DatabaseManager.exists("SELECT 1 FROM Farm WHERE farmer_id = 1"));
        assertFalse(DatabaseManager.exists("SELECT 1 FROM Cultivation WHERE farm_id IN (1, 2)"));
    }
}
