package com.cbec.ai.pipeline;

import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;

import java.io.File;

public class InspectSouthKoreaExcelTest {

    @Test
    void inspectKoreaExcelFiles() throws Exception {
        File file1 = new File("관세청_HS부호_20260101.xlsx");
        System.out.println("File 1 exists: " + file1.exists() + ", Size: " + file1.length());

        try (Workbook wb1 = WorkbookFactory.create(file1)) {
            System.out.println("Workbook 1 Sheet Count: " + wb1.getNumberOfSheets());
            Sheet sheet1 = wb1.getSheetAt(0);
            System.out.println("Sheet 1 Name: " + sheet1.getSheetName() + ", Total Rows: " + sheet1.getLastRowNum());
            Row header1 = sheet1.getRow(0);
            if (header1 != null) {
                System.out.println("--- FILE 1 HEADERS ---");
                for (Cell c : header1) {
                    System.out.print("[" + c.getColumnIndex() + "] " + c.getStringCellValue() + "  |  ");
                }
                System.out.println();
            }
            Row row1 = sheet1.getRow(1);
            if (row1 != null) {
                System.out.println("--- FILE 1 ROW 1 ---");
                for (Cell c : row1) {
                    System.out.print("[" + c.getColumnIndex() + "] " + c.toString() + "  |  ");
                }
                System.out.println();
            }
        }

        File file2 = new File("관세청_품목번호별 관세율표_20260211.xlsx");
        System.out.println("\nFile 2 exists: " + file2.exists() + ", Size: " + file2.length());

        try (Workbook wb2 = WorkbookFactory.create(file2)) {
            System.out.println("Workbook 2 Sheet Count: " + wb2.getNumberOfSheets());
            Sheet sheet2 = wb2.getSheetAt(0);
            System.out.println("Sheet 2 Name: " + sheet2.getSheetName() + ", Total Rows: " + sheet2.getLastRowNum());
            Row header2 = sheet2.getRow(0);
            if (header2 != null) {
                System.out.println("--- FILE 2 HEADERS ---");
                for (Cell c : header2) {
                    System.out.print("[" + c.getColumnIndex() + "] " + c.getStringCellValue() + "  |  ");
                }
                System.out.println();
            }
            Row row2 = sheet2.getRow(1);
            if (row2 != null) {
                System.out.println("--- FILE 2 ROW 1 ---");
                for (Cell c : row2) {
                    System.out.print("[" + c.getColumnIndex() + "] " + c.toString() + "  |  ");
                }
                System.out.println();
            }
        }
    }
}
