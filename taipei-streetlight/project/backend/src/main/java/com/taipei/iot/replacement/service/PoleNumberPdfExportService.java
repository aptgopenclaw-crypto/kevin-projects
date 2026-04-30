package com.taipei.iot.replacement.service;

import com.taipei.iot.replacement.entity.LightPoleNumber;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PoleNumberPdfExportService {

    private final QrCodeService qrCodeService;

    private static final int COLS = 4;
    private static final int ROWS = 5;
    private static final int ITEMS_PER_PAGE = COLS * ROWS;
    private static final float QR_SIZE = 100f;
    private static final float CELL_PADDING = 10f;
    private static final float LABEL_FONT_SIZE = 9f;
    private static final float TITLE_FONT_SIZE = 14f;
    private static final float HEADER_HEIGHT = 40f;

    public byte[] exportPdf(List<LightPoleNumber> poleNumbers, String baseUrl) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDFont font = loadFont(doc);

            int totalPages = (int) Math.ceil((double) poleNumbers.size() / ITEMS_PER_PAGE);
            if (totalPages == 0) totalPages = 1;

            for (int pageIdx = 0; pageIdx < totalPages; pageIdx++) {
                int start = pageIdx * ITEMS_PER_PAGE;
                int end = Math.min(start + ITEMS_PER_PAGE, poleNumbers.size());
                List<LightPoleNumber> pageItems = poleNumbers.subList(start, end);

                addPage(doc, font, pageItems, baseUrl, pageIdx + 1, totalPages);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private void addPage(PDDocument doc, PDFont font, List<LightPoleNumber> items,
                         String baseUrl, int pageNum, int totalPages) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        float pageWidth = page.getMediaBox().getWidth();   // 595
        float pageHeight = page.getMediaBox().getHeight();  // 842

        float cellWidth = (pageWidth - 2 * CELL_PADDING) / COLS;
        float cellHeight = (pageHeight - HEADER_HEIGHT - 2 * CELL_PADDING) / ROWS;

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            // Title
            cs.beginText();
            cs.setFont(font, TITLE_FONT_SIZE);
            cs.newLineAtOffset(CELL_PADDING, pageHeight - HEADER_HEIGHT + 10);
            cs.showText("臺北市路燈號碼牌 QR Code — 第 " + pageNum + "/" + totalPages + " 頁");
            cs.endText();

            // Grid
            for (int i = 0; i < items.size(); i++) {
                LightPoleNumber pole = items.get(i);
                int col = i % COLS;
                int row = i / COLS;

                float cellX = CELL_PADDING + col * cellWidth;
                float cellY = pageHeight - HEADER_HEIGHT - CELL_PADDING - (row + 1) * cellHeight;

                // Center QR code in cell
                float qrX = cellX + (cellWidth - QR_SIZE) / 2;
                float qrY = cellY + cellHeight - QR_SIZE - 5;

                String content = baseUrl + "/public/repair?pole=" + pole.getPoleNumber();
                byte[] qrPng = qrCodeService.generatePng(content, 300);

                PDImageXObject qrImage = PDImageXObject.createFromByteArray(doc, qrPng, "qr-" + pole.getId());
                cs.drawImage(qrImage, qrX, qrY, QR_SIZE, QR_SIZE);

                // Label below QR code
                String label = pole.getPoleNumber();
                float labelWidth = font.getStringWidth(label) / 1000 * LABEL_FONT_SIZE;
                float labelX = cellX + (cellWidth - labelWidth) / 2;
                float labelY = qrY - LABEL_FONT_SIZE - 4;

                cs.beginText();
                cs.setFont(font, LABEL_FONT_SIZE);
                cs.newLineAtOffset(labelX, labelY);
                cs.showText(label);
                cs.endText();
            }
        }
    }

    private PDFont loadFont(PDDocument doc) throws IOException {
        ClassPathResource resource = new ClassPathResource("fonts/NotoSansTC-Regular.ttf");
        try (InputStream is = resource.getInputStream()) {
            return PDType0Font.load(doc, is);
        }
    }
}
