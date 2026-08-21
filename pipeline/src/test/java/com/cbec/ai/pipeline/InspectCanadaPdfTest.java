package com.cbec.ai.pipeline;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.File;

public class InspectCanadaPdfTest {

    @Test
    void inspectPage27() throws Exception {
        File file = new File("01-99-2026-eng.pdf");
        try (PDDocument doc = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(27);
            stripper.setEndPage(27);
            String text = stripper.getText(doc);

            System.out.println("==================================================");
            System.out.println("--- PAGE 27 ---");
            System.out.println("==================================================");
            System.out.println(text);
        }
    }
}
