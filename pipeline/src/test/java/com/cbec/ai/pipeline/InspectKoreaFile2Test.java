package com.cbec.ai.pipeline;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;

import java.io.File;

public class InspectKoreaFile2Test {

    @Test
    void inspectKoreaFile2() throws Exception {
        File file2 = new File("관세청_품목번호별 관세율표_20260211.xlsx");
        System.out.println("File 2 Size: " + file2.length());

        try (Workbook wb2 = WorkbookFactory.create(file2)) {
            System.out.println("Workbook 2 Sheet Count: " + wb2.getNumberOfSheets());
            Sheet sheet2 = wb2.getSheetAt(0);
            System.out.println("Sheet 2 Name: " + sheet2.getSheetName() + ", Total Rows: " + sheet2.getLastRowNum());
            Row header2 = sheet2.getRow(0);
            if (header2 != null) {
                System.out.println("--- FILE 2 HEADERS ---");
                for (Cell c : header2) {
                    System.out.println("[" + c.getColumnIndex() + "] " + c.getStringCellValue());
                }
            }
            Row row2 = sheet2.getRow(1);
            if (row2 != null) {
                System.out.println("--- FILE 2 ROW 1 ---");
                for (Cell c : row2) {
                    System.out.println("[" + c.getColumnIndex() + "] " + c.toString());
                }
            }
        }
    }
}
