package com.agri.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Prints the result of any SELECT as an aligned text table. */
public final class TablePrinter {

    private TablePrinter() {
    }

    public static void print(String title, String sql) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();
            String[] headers = new String[columns];
            for (int i = 0; i < columns; i++) {
                headers[i] = meta.getColumnLabel(i + 1);
            }

            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                String[] row = new String[columns];
                for (int i = 0; i < columns; i++) {
                    row[i] = format(rs.getObject(i + 1));
                }
                rows.add(row);
            }

            int[] width = new int[columns];
            for (int i = 0; i < columns; i++) {
                width[i] = headers[i].length();
                for (String[] row : rows) {
                    width[i] = Math.max(width[i], row[i].length());
                }
            }

            System.out.println();
            System.out.println("=== " + title + " ===");
            if (rows.isEmpty()) {
                System.out.println("(no records)");
                return;
            }
            printRow(headers, width);
            StringBuilder line = new StringBuilder();
            for (int w : width) {
                line.append("-".repeat(w)).append("-+-");
            }
            System.out.println(line.substring(0, line.length() - 3));
            for (String[] row : rows) {
                printRow(row, width);
            }
            System.out.println("(" + rows.size() + " row" + (rows.size() == 1 ? "" : "s") + ")");
        }
    }

    private static void printRow(String[] cells, int[] width) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            sb.append(String.format("%-" + width[i] + "s", cells[i]));
            if (i < cells.length - 1) {
                sb.append(" | ");
            }
        }
        System.out.println(sb);
    }

    private static String format(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof Double d) {
            return String.format("%,.2f", d);
        }
        return value.toString();
    }
}
