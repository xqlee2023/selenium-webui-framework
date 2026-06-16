package com.framework.utils.format;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/** Excel data provider utility (.xlsx). */
public final class ExcelUtils {

    private ExcelUtils() {}

    /**
     * Read an Excel sheet into a List of Maps (header → value).
     * @param filePath path to .xlsx file
     * @param sheetName sheet name (0-indexed number or name)
     */
    public static List<Map<String, String>> readSheet(String filePath, Object sheetName) {
        List<Map<String, String>> data = new ArrayList<>();
        try (InputStream is = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = sheetName instanceof Integer
                    ? workbook.getSheetAt((Integer) sheetName)
                    : workbook.getSheet(sheetName.toString());

            if (sheet == null) {
                throw new IllegalArgumentException("Sheet not found: " + sheetName);
            }

            Row headerRow = sheet.getRow(0);
            int colCount = headerRow.getLastCellNum();
            String[] headers = new String[colCount];
            for (int i = 0; i < colCount; i++) {
                headers[i] = getCellValue(headerRow.getCell(i));
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> rowMap = new LinkedHashMap<>();
                boolean hasData = false;
                for (int c = 0; c < colCount; c++) {
                    String value = getCellValue(row.getCell(c));
                    rowMap.put(headers[c], value);
                    if (!value.isEmpty()) hasData = true;
                }
                if (hasData) data.add(rowMap);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to read Excel: " + filePath, e);
        }
        return data;
    }

    /** Read all sheets into a map of sheetName → data. */
    public static Map<String, List<Map<String, String>>> readAllSheets(String filePath) {
        Map<String, List<Map<String, String>>> allData = new LinkedHashMap<>();
        try (InputStream is = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(is)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                String sheetName = workbook.getSheetName(i);
                allData.put(sheetName, readSheet(filePath, sheetName));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Excel: " + filePath, e);
        }
        return allData;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.getStringCellValue();
                }
            }
            default -> "";
        };
    }
}
