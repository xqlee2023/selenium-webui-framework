package com.framework.utils.format;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

/** CSV data provider utility. */
public final class CsvUtils {

    private CsvUtils() {}

    /** Read CSV file into list of maps (header → value). */
    public static List<Map<String, String>> readCsv(String filePath) {
        List<Map<String, String>> data = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            String[] headers = reader.readNext();
            if (headers == null) return data;

            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, String> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < headers.length && i < line.length; i++) {
                    rowMap.put(headers[i].trim(), line[i].trim());
                }
                data.add(rowMap);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read CSV: " + filePath, e);
        }
        return data;
    }

    /** Write data to CSV. */
    public static void writeCsv(String filePath, List<Map<String, String>> data) {
        if (data.isEmpty()) return;
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            String[] headers = data.get(0).keySet().toArray(new String[0]);
            writer.writeNext(headers);
            for (Map<String, String> row : data) {
                String[] values = new String[headers.length];
                for (int i = 0; i < headers.length; i++) {
                    values[i] = row.getOrDefault(headers[i], "");
                }
                writer.writeNext(values);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to write CSV: " + filePath, e);
        }
    }
}
