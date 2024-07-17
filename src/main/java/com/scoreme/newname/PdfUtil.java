package com.scoreme.newname;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public class PdfUtil {

    private static final Logger logger = LoggerFactory.getLogger(PdfUtil.class);

    public static String extractText(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            StringBuilder text = new StringBuilder();

            // Iterate through all pages and extract text
            if(document.getNumberOfPages() >= 3)
            {
                for (int i = 0; i <= 3; i++) {
                    pdfStripper.setStartPage(i);
                    pdfStripper.setEndPage(i);
                    text.append(pdfStripper.getText(document));
                }
            }
            else{
                for (int i = 1; i <= document.getNumberOfPages(); i++) {
                    pdfStripper.setStartPage(i);
                    pdfStripper.setEndPage(i);
                    text.append(pdfStripper.getText(document));
                }
            }

            return text.toString();
        }
    }


    public static String extractOcrTextFromPage(PDPage page) throws IOException, TesseractException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(page);
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            BufferedImage image = pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);

            ITesseract tesseract = new Tesseract();
            return tesseract.doOCR(image);
        }
    }
}
