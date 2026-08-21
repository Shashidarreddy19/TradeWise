package com.cbec.ai.pipeline;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.File;

public class PdfInspectionTest {

    @Test
    void inspectPdfContent() throws Exception {
        File pdfFile = new File("ITC-HS_2022.pdf");
        if (!pdfFile.exists()) {
            System.out.println("PDF file not found at " + pdfFile.getAbsolutePath());
            return;
        }

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            System.out.println("Total PDF Pages: " + document.getNumberOfPages());
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(6);
            stripper.setEndPage(10);

            String text = stripper.getText(document);
            System.out.println("--- SAMPLE TEXT FROM PAGES 6-10 ---");
            String[] lines = text.split("\\r?\\n");
            for (int i = 0; i < Math.min(80, lines.length); i++) {
                System.out.println("[" + i + "] " + lines[i]);
            }
        }
    }
}
