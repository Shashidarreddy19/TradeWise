package com.cbec.ai.pipeline;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.junit.jupiter.api.Test;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.InputStream;

public class InspectKoreaFile2HeadersTest {

    @Test
    void inspectHeadersStreaming() throws Exception {
        File file2 = new File("관세청_품목번호별 관세율표_20260211.xlsx");
        System.out.println("File 2 size: " + file2.length());

        try (OPCPackage pkg = OPCPackage.open(file2)) {
            XSSFReader reader = new XSSFReader(pkg);
            SharedStringsTable sst = (SharedStringsTable) reader.getSharedStringsTable();

            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(new DefaultHandler() {
                private String lastContents = "";
                private boolean isString = false;
                private int rowNum = 0;
                private int colNum = 0;

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    if ("row".equals(qName)) {
                        rowNum++;
                        colNum = 0;
                    }
                    if ("c".equals(qName)) {
                        String cellType = attributes.getValue("t");
                        isString = "s".equals(cellType);
                    }
                    lastContents = "";
                }

                @Override
                public void characters(char[] ch, int start, int length) {
                    lastContents += new String(ch, start, length);
                }

                @Override
                public void endElement(String uri, String localName, String qName) {
                    if (isString && !lastContents.isEmpty()) {
                        try {
                            int idx = Integer.parseInt(lastContents);
                            lastContents = sst.getItemAt(idx).getString();
                        } catch (Exception ignored) {}
                    }
                    if ("v".equals(qName) || "t".equals(qName)) {
                        if (rowNum <= 2) {
                            System.out.println("Row " + rowNum + " Cell [" + colNum + "]: " + lastContents);
                        }
                        colNum++;
                    }
                }
            });

            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
            if (sheets.hasNext()) {
                try (InputStream sheetStream = sheets.next()) {
                    parser.parse(new InputSource(sheetStream));
                }
            }
        }
    }
}
