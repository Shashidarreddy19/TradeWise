package com.cbec.ai.pipeline;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;

public class InspectUaeExcelTest {

    @Test
    void inspectExcelFile() throws Exception {
        File file = new File("HSCodeMaster-v3.3customers.xlsx");
        System.out.println("File exists: " + file.exists() + ", Size: " + file.length());

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            int sheetCount = workbook.getNumberOfSheets();
            System.out.println("Total Worksheets: " + sheetCount);

            for (int i = 0; i < sheetCount; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                System.out.println("Sheet " + i + ": " + sheet.getSheetName() + " (Physical Rows: " + sheet.getPhysicalNumberOfRows() + ")");

                if (sheet.getPhysicalNumberOfRows() > 0) {
                    Row headerRow = sheet.getRow(sheet.getFirstRowNum());
                    if (headerRow != null) {
                        System.out.print("   Columns: ");
                        for (Cell cell : headerRow) {
                            System.out.print("[" + cell.toString() + "] ");
                        }
                        System.out.println();
                    }
                }
            }
        }
    }
}
