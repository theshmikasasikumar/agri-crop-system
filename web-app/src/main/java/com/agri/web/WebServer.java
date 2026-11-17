
package com.agri.web;

import com.agri.service.RecordService;
import com.agri.util.DatabaseManager;
import com.agri.util.ValidationException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.time.LocalDate;
import java.util.*;

public final class WebServer {

    private static final Gson GSON = new Gson();
    private static final RecordService RECORDS = new RecordService();

    private WebServer() { }

    public static void main(String[] args) throws Exception {

        DatabaseManager.initializeSchema();

        int port = Integer.parseInt(
                System.getenv().getOrDefault("PORT", "8080")
        );

        HttpServer server = HttpServer.create(
                new InetSocketAddress("0.0.0.0", port), 0
        );

        server.createContext("/api/", WebServer::api);
        server.createContext("/", WebServer::staticFiles);

        server.setExecutor(
                java.util.concurrent.Executors.newCachedThreadPool()
        );

        server.start();

        System.out.println(
                "Agri Crop Management website running at http://localhost:" + port
        );
    }

    private static void api(HttpExchange ex) throws IOException {

        addCors(ex);

        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        try {

            String path = ex.getRequestURI().getPath()
                    .substring("/api/".length());

            String method = ex.getRequestMethod();

            Object result;

            if (method.equals("GET") && path.equals("dashboard")) {
                result = dashboard();

            } else if (method.equals("GET") && path.startsWith("reports/")) {
                result = report(path.substring(8));

            } else if (method.equals("GET") && TABLES.containsKey(path)) {
                result = query("SELECT * FROM " + TABLES.get(path));

            } else if (method.equals("POST")) {
                result = create(path, body(ex));

            } else if (method.equals("PUT") && path.startsWith("farmers/")) {

                int id = Integer.parseInt(path.substring(8));

                RECORDS.updateFarmerContact(
                        id,
                        body(ex).get("contactNumber").getAsString()
                );

                result = Map.of("message", "Farmer contact updated");

            } else if (method.equals("DELETE") && path.startsWith("farmers/")) {

                RECORDS.deleteFarmer(
                        Integer.parseInt(path.substring(8))
                );

                result = Map.of("message", "Farmer deleted");

            } else {

                send(ex, 404, Map.of("error", "API endpoint not found"));
                return;
            }

            send(ex, 200, result);

        } catch (Exception e) {

            int status =
                    (e instanceof ValidationException
                    || e instanceof IllegalArgumentException
                    || e instanceof IllegalStateException)
                    ? 400 : 500;

            send(
                    ex,
                    status,
                    Map.of(
                            "error",
                            Optional.ofNullable(e.getMessage())
                                    .orElse("Request failed")
                    )
            );
        }
    }

    private static final Map<String, String> TABLES = Map.of(
            "farmers", "Farmer",
            "farms", "Farm",
            "crops", "Crop",
            "cultivations", "Cultivation",
            "fertilizers", "FertilizerInput",
            "irrigation", "Irrigation",
            "harvests", "Harvest"
    );

    private static Object create(String path, JsonObject j)
            throws SQLException {

        return switch (path) {

            case "farmers" ->
                    Map.of(
                            "id",
                            RECORDS.addFarmer(
                                    s(j, "name"),
                                    s(j, "contactNumber"),
                                    s(j, "address")
                            )
                    );

            case "farms" ->
                    Map.of(
                            "id",
                            RECORDS.addFarm(
                                    i(j, "farmerId"),
                                    s(j, "location"),
                                    d(j, "totalArea")
                            )
                    );

            case "crops" ->
                    Map.of(
                            "id",
                            RECORDS.addCrop(
                                    s(j, "cropName"),
                                    s(j, "category"),
                                    i(j, "durationDays")
                            )
                    );

            case "cultivations" ->
                    Map.of(
                            "id",
                            RECORDS.registerCultivation(
                                    i(j, "farmId"),
                                    i(j, "cropId"),
                                    LocalDate.parse(s(j, "cultivationDate")),
                                    d(j, "cultivatedArea"),
                                    s(j, "season")
                            )
                    );

            case "fertilizers" ->
                    Map.of(
                            "id",
                            RECORDS.addFertilizerInput(
                                    i(j, "cultivationId"),
                                    s(j, "fertilizerType"),
                                    d(j, "quantityKg"),
                                    d(j, "inputCost"),
                                    LocalDate.parse(s(j, "applicationDate"))
                            )
                    );

            case "irrigation" ->
                    Map.of(
                            "id",
                            RECORDS.addIrrigation(
                                    i(j, "cultivationId"),
                                    LocalDate.parse(s(j, "irrigationDate")),
                                    d(j, "waterQuantity"),
                                    s(j, "method")
                            )
                    );

            case "harvests" ->
                    Map.of(
                            "id",
                            RECORDS.addHarvest(
                                    i(j, "cultivationId"),
                                    LocalDate.parse(s(j, "harvestDate")),
                                    d(j, "quantityKg"),
                                    s(j, "quality"),
                                    d(j, "sellingPrice")
                            )
                    );

            default ->
                    throw new IllegalArgumentException("Unknown record type");
        };
    }

    private static Object dashboard() throws SQLException {

        Map<String, Object> m = new LinkedHashMap<>();

        m.put("farmers", scalar("SELECT COUNT(*) FROM Farmer"));

        m.put("farms", scalar("SELECT COUNT(*) FROM Farm"));

        m.put("crops", scalar("SELECT COUNT(*) FROM Crop"));

        m.put("cultivations", scalar("SELECT COUNT(*) FROM Cultivation"));

        m.put(
                "harvestKg",
                scalar("SELECT COALESCE(SUM(quantity_kg), 0) FROM Harvest")
        );

        m.put(
                "estimatedProfit",
                scalar(
                        "SELECT COALESCE(SUM(profit), 0) " +
                        "FROM v_cultivation_summary " +
                        "WHERE quantity_kg IS NOT NULL"
                )
        );

        m.put(
                "recent",
                query(
                        "SELECT cultivation_id, crop_name, farmer_name, season, " +
                        "quantity_kg, profit " +
                        "FROM v_cultivation_summary " +
                        "ORDER BY cultivation_id DESC LIMIT 6"
                )
        );

        // PostgreSQL-compatible numeric casting for ROUND()
        m.put(
                "cropYield",
                query(
                        "SELECT crop_name AS crop, " +
                        "ROUND(" +
                        "SUM(quantity_kg)::numeric / " +
                        "NULLIF(SUM(cultivated_area)::numeric, 0), 2" +
                        ") AS yield " +
                        "FROM v_cultivation_summary " +
                        "WHERE quantity_kg IS NOT NULL " +
                        "GROUP BY crop_name " +
                        "ORDER BY yield DESC"
                )
        );

        return m;
    }

    private static Object report(String name) throws SQLException {

        String sql = switch (name) {

            case "crop-yield" ->
                    "SELECT crop_name AS crop, " +
                    "COUNT(*) AS harvests, " +
                    "ROUND(SUM(quantity_kg)::numeric, 1) AS total_kg, " +
                    "ROUND(" +
                    "SUM(quantity_kg)::numeric / " +
                    "NULLIF(SUM(cultivated_area)::numeric, 0), 2" +
                    ") AS yield_kg_per_acre " +
                    "FROM v_cultivation_summary " +
                    "WHERE quantity_kg IS NOT NULL " +
                    "GROUP BY crop_name " +
                    "ORDER BY yield_kg_per_acre DESC";

            case "farm-production" ->
                    "SELECT farm_id, location, farmer_name AS farmer, " +
                    "COUNT(*) AS harvests, " +
                    "ROUND(SUM(quantity_kg)::numeric, 1) " +
                    "AS total_production_kg, " +
                    "ROUND(SUM(revenue)::numeric, 2) AS total_revenue " +
                    "FROM v_cultivation_summary " +
                    "WHERE quantity_kg IS NOT NULL " +
                    "GROUP BY farm_id, location, farmer_name " +
                    "ORDER BY total_production_kg DESC";

            case "input-costs" ->
                    "SELECT cr.crop_name AS crop, " +
                    "ROUND(SUM(fi.quantity_kg)::numeric, 1) " +
                    "AS fertilizer_kg, " +
                    "ROUND(SUM(fi.input_cost)::numeric, 2) " +
                    "AS total_input_cost " +
                    "FROM FertilizerInput fi " +
                    "JOIN Cultivation c " +
                    "ON c.cultivation_id = fi.cultivation_id " +
                    "JOIN Crop cr ON cr.crop_id = c.crop_id " +
                    "GROUP BY cr.crop_name " +
                    "ORDER BY total_input_cost DESC";

            case "seasonal" ->
                    "SELECT season, COUNT(*) AS harvests, " +
                    "ROUND(SUM(quantity_kg)::numeric, 1) " +
                    "AS total_harvest_kg, " +
                    "ROUND(SUM(revenue)::numeric, 2) AS revenue, " +
                    "ROUND(SUM(total_input_cost)::numeric, 2) AS input_cost, " +
                    "ROUND(SUM(profit)::numeric, 2) AS profit " +
                    "FROM v_cultivation_summary " +
                    "WHERE quantity_kg IS NOT NULL " +
                    "GROUP BY season " +
                    "ORDER BY season";

            default ->
                    throw new IllegalArgumentException("Unknown report");
        };

        return query(sql);
    }

    private static List<Map<String, Object>> query(String sql)
            throws SQLException {

        List<Map<String, Object>> rows = new ArrayList<>();

        try (
                Connection c = DatabaseManager.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {

            ResultSetMetaData md = rs.getMetaData();

            int n = md.getColumnCount();

            while (rs.next()) {

                Map<String, Object> row = new LinkedHashMap<>();

                for (int k = 1; k <= n; k++) {
                    row.put(md.getColumnLabel(k), rs.getObject(k));
                }

                rows.add(row);
            }
        }

        return rows;
    }

    private static Object scalar(String sql) throws SQLException {

        List<Map<String, Object>> r = query(sql);

        return r.isEmpty()
                ? 0
                : r.get(0).values().iterator().next();
    }

    private static JsonObject body(HttpExchange ex)
            throws IOException {

        return GSON.fromJson(
                new String(
                        ex.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8
                ),
                JsonObject.class
        );
    }

    private static String s(JsonObject j, String k) {
        return j.get(k).getAsString().trim();
    }

    private static int i(JsonObject j, String k) {
        return j.get(k).getAsInt();
    }

    private static double d(JsonObject j, String k) {
        return j.get(k).getAsDouble();
    }

    private static void send(
            HttpExchange ex,
            int status,
            Object data
    ) throws IOException {

        byte[] b = GSON.toJson(data)
                .getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=utf-8"
        );

        ex.sendResponseHeaders(status, b.length);

        ex.getResponseBody().write(b);
        ex.close();
    }

    private static void addCors(HttpExchange ex) {

        ex.getResponseHeaders().set(
                "Access-Control-Allow-Origin", "*"
        );

        ex.getResponseHeaders().set(
                "Access-Control-Allow-Methods",
                "GET,POST,PUT,DELETE,OPTIONS"
        );

        ex.getResponseHeaders().set(
                "Access-Control-Allow-Headers",
                "Content-Type"
        );
    }

    private static void staticFiles(HttpExchange ex)
            throws IOException {

        String requested = ex.getRequestURI().getPath();

        if (requested.equals("/")) {
            requested = "/index.html";
        }

        Path webDir = Files.isDirectory(Path.of("web"))
                ? Path.of("web")
                : Path.of("../web");

        Path file = webDir.resolve(
                requested.substring(1)
        ).normalize();

        if (!file.startsWith(webDir.normalize())
                || !Files.isRegularFile(file)) {

            byte[] b = "Not found".getBytes(StandardCharsets.UTF_8);

            ex.sendResponseHeaders(404, b.length);
            ex.getResponseBody().write(b);
            ex.close();

            return;
        }

        String type =
                requested.endsWith(".css")
                ? "text/css"
                : requested.endsWith(".js")
                ? "application/javascript"
                : "text/html";

        byte[] b = Files.readAllBytes(file);

        ex.getResponseHeaders().set(
                "Content-Type",
                type + "; charset=utf-8"
        );

        ex.sendResponseHeaders(200, b.length);

        ex.getResponseBody().write(b);
        ex.close();
    }
}
