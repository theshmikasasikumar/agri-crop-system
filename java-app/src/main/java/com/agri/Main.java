package com.agri;

import com.agri.service.RecordService;
import com.agri.service.ReportService;
import com.agri.util.Constants;
import com.agri.util.DatabaseManager;
import com.agri.util.InputHelper;
import com.agri.util.ValidationException;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Console menu for the Agriculture Crop Management and Yield Analysis System. */
public class Main {
    private static final InputHelper in = new InputHelper(new Scanner(System.in));
    private static final RecordService records = new RecordService();
    private static final ReportService reports = new ReportService();

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println(" Agriculture Crop Management and Yield Analysis System");
        System.out.println("==========================================================");
        try {
            DatabaseManager.initializeSchema();
            System.out.println("Database: " + DatabaseManager.dbFile().toAbsolutePath().normalize());
        } catch (SQLException | IOException e) {
            System.out.println("Could not open the database: " + e.getMessage());
            return;
        }

        try {
            boolean running = true;
            while (running) {
                printMenu();
                running = handle(in.readInt("Enter choice: ", 0, 13));
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nInput ended. Goodbye!");
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("---------------------- MAIN MENU ----------------------");
        System.out.println("  1. Add farmer");
        System.out.println("  2. Add farm");
        System.out.println("  3. Add crop");
        System.out.println("  4. Register cultivation");
        System.out.println("  5. Add fertilizer input");
        System.out.println("  6. Record irrigation");
        System.out.println("  7. Record harvest");
        System.out.println("  8. View records");
        System.out.println("  9. Productivity and profit of a cultivation");
        System.out.println(" 10. Reports (crop-wise, farm-wise, input costs, seasonal)");
        System.out.println(" 11. Update farmer contact number");
        System.out.println(" 12. Delete farmer");
        System.out.println(" 13. Load sample data (empty database only)");
        System.out.println("  0. Exit");
    }

    /** Runs one menu action. Returns false when the user chose Exit. */
    private static boolean handle(int choice) {
        try {
            switch (choice) {
                case 1 -> addFarmer();
                case 2 -> addFarm();
                case 3 -> addCrop();
                case 4 -> registerCultivation();
                case 5 -> addFertilizerInput();
                case 6 -> addIrrigation();
                case 7 -> addHarvest();
                case 8 -> viewRecords();
                case 9 -> productivity();
                case 10 -> reportsMenu();
                case 11 -> updateContact();
                case 12 -> deleteFarmer();
                case 13 -> loadSampleData();
                case 0 -> {
                    System.out.println("Goodbye!");
                    return false;
                }
                default -> System.out.println("Invalid choice.");
            }
        } catch (ValidationException e) {
            System.out.println("  Error: " + e.getMessage());
        } catch (SQLException | IOException e) {
            System.out.println("  Database error: " + e.getMessage());
        }
        return true;
    }

    // ------------------------------------------------------------- add records
    private static void addFarmer() throws SQLException {
        String name = in.readNonEmpty("Farmer name: ");
        String phone = in.readPhone("Contact number (10 digits): ");
        String address = in.readNonEmpty("Address: ");
        int id = records.addFarmer(name, phone, address);
        System.out.println("  Farmer added with id " + id);
    }

    private static void addFarm() throws SQLException {
        reports.listTable(1);
        int farmerId = in.readId("Farmer id: ");
        String location = in.readNonEmpty("Farm location: ");
        double area = in.readPositiveDouble("Total cultivated area (acres): ");
        int id = records.addFarm(farmerId, location, area);
        System.out.println("  Farm added with id " + id);
    }

    private static void addCrop() throws SQLException {
        String name = in.readNonEmpty("Crop name: ");
        String category = in.readChoice("Crop category", Constants.CATEGORIES);
        int days = in.readInt("Expected cultivation duration (days): ", 1, 800);
        int id = records.addCrop(name, category, days);
        System.out.println("  Crop added with id " + id);
    }

    private static void registerCultivation() throws SQLException {
        reports.listTable(2);
        reports.listTable(3);
        int farmId = in.readId("Farm id: ");
        int cropId = in.readId("Crop id: ");
        LocalDate date = in.readDate("Cultivation date (yyyy-MM-dd): ");
        double area = in.readPositiveDouble("Cultivated area (acres): ");
        String season = in.readChoice("Season", Constants.SEASONS);
        int id = records.registerCultivation(farmId, cropId, date, area, season);
        System.out.println("  Cultivation registered with id " + id);
    }

    private static void addFertilizerInput() throws SQLException {
        reports.listTable(4);
        int cultivationId = in.readId("Cultivation id: ");
        String type = in.readNonEmpty("Fertilizer type: ");
        double qty = in.readPositiveDouble("Quantity used (kg): ");
        double cost = in.readNonNegativeDouble("Input cost (INR): ");
        LocalDate date = in.readDate("Application date (yyyy-MM-dd): ");
        int id = records.addFertilizerInput(cultivationId, type, qty, cost, date);
        System.out.println("  Fertilizer input saved with id " + id);
    }

    private static void addIrrigation() throws SQLException {
        reports.listTable(4);
        int cultivationId = in.readId("Cultivation id: ");
        LocalDate date = in.readDate("Irrigation date (yyyy-MM-dd): ");
        double litres = in.readPositiveDouble("Water quantity (litres): ");
        String method = in.readChoice("Irrigation method", Constants.IRRIGATION_METHODS);
        int id = records.addIrrigation(cultivationId, date, litres, method);
        System.out.println("  Irrigation saved with id " + id);
    }

    private static void addHarvest() throws SQLException {
        reports.listTable(4);
        int cultivationId = in.readId("Cultivation id: ");
        LocalDate date = in.readDate("Harvest date (yyyy-MM-dd): ");
        double qty = in.readPositiveDouble("Quantity harvested (kg): ");
        String quality = in.readChoice("Crop quality category", Constants.QUALITIES);
        double price = in.readPositiveDouble("Selling price (INR per kg): ");
        int id = records.addHarvest(cultivationId, date, qty, quality, price);
        System.out.println("  Harvest saved with id " + id);
    }

    // ------------------------------------------------------- view / calculate
    private static void viewRecords() throws SQLException {
        System.out.println(" 1 Farmers  2 Farms  3 Crops  4 Cultivations  5 Fertilizer inputs"
                + "  6 Irrigation  7 Harvests  0 Back");
        int choice = in.readInt("View which records? ", 0, 7);
        if (choice != 0) {
            reports.listTable(choice);
        }
    }

    private static void productivity() throws SQLException {
        reports.listTable(4);
        int id = in.readId("Cultivation id: ");
        System.out.println(reports.calculate(id).toText());
    }

    private static void reportsMenu() throws SQLException {
        System.out.println(" 1 Crop-wise yield  2 Farm-wise production  3 Input costs  4 Seasonal summary  0 Back");
        int choice = in.readInt("Which report? ", 0, 4);
        switch (choice) {
            case 1 -> reports.cropWiseYield();
            case 2 -> reports.farmWiseProduction();
            case 3 -> reports.inputCosts();
            case 4 -> reports.seasonalSummary();
            default -> { /* back */ }
        }
    }

    // -------------------------------------------------------- maintenance
    private static void updateContact() throws SQLException {
        reports.listTable(1);
        int farmerId = in.readId("Farmer id: ");
        String phone = in.readPhone("New contact number (10 digits): ");
        records.updateFarmerContact(farmerId, phone);
        System.out.println("  Contact number updated.");
    }

    private static void deleteFarmer() throws SQLException {
        reports.listTable(1);
        int farmerId = in.readId("Farmer id to delete: ");
        String sure = in.readChoice("This also deletes the farmer's farms and all their records. Continue?",
                List.of("yes", "no"));
        if (sure.equals("yes")) {
            records.deleteFarmer(farmerId);
            System.out.println("  Farmer deleted.");
        } else {
            System.out.println("  Cancelled.");
        }
    }

    private static void loadSampleData() throws SQLException, IOException {
        if (DatabaseManager.exists("SELECT 1 FROM Farmer")) {
            System.out.println("  The database already contains data. To reset it, run: python database/setup_db.py");
            return;
        }
        DatabaseManager.loadSampleData();
        System.out.println("  Sample data loaded.");
    }
}
