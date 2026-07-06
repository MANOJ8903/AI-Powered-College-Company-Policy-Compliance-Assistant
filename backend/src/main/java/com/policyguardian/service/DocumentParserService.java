package com.policyguardian.service;

import com.policyguardian.dto.TextChunk;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentParserService {

    public String parsePdf(InputStream inputStream) throws IOException {
        byte[] bytes = toByteArray(inputStream);
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    public String parseDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    public List<TextChunk> chunkText(String text, int chunkSize, int overlap) {
        List<TextChunk> chunks = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");

        StringBuilder currentBuffer = new StringBuilder();
        String currentSectionName = "General / Introduction";
        int chunkIndex = 0;

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            if (isSectionHeader(trimmedLine)) {
                // Flush the buffer before starting a new section, if it contains content
                if (currentBuffer.length() > 0) {
                    chunks.add(new TextChunk(currentBuffer.toString().trim(), chunkIndex++, currentSectionName));
                    currentBuffer.setLength(0);
                }
                currentSectionName = trimmedLine;
                continue;
            }

            if (currentBuffer.length() > 0) {
                currentBuffer.append(" ");
            }
            currentBuffer.append(trimmedLine);

            // While the buffer exceeds the target chunk size, slide and output chunks
            while (currentBuffer.length() >= chunkSize) {
                String chunkContent = currentBuffer.substring(0, chunkSize);
                chunks.add(new TextChunk(chunkContent.trim(), chunkIndex++, currentSectionName));

                int startOfOverlap = chunkSize - overlap;
                if (startOfOverlap < currentBuffer.length()) {
                    currentBuffer.delete(0, startOfOverlap);
                } else {
                    currentBuffer.setLength(0);
                }
            }
        }

        // Flush any remaining content
        if (currentBuffer.length() > 0) {
            chunks.add(new TextChunk(currentBuffer.toString().trim(), chunkIndex++, currentSectionName));
        }

        return chunks;
    }

    private boolean isSectionHeader(String line) {
        line = line.trim();
        if (line.isEmpty() || line.length() > 80) return false;

        // Matches headers starting with Section, Rule, Chapter, Article, Policy, or Clause
        if (line.matches("^(?i)(Section|Rule|Chapter|Article|Policy|Clause)\\s+\\d+.*") ||
            line.matches("^(?i)(Section|Rule|Chapter|Article|Policy|Clause)\\s+[A-Z].*")) {
            return true;
        }

        // Matches uppercase lines that look like short headers
        if (line.equals(line.toUpperCase()) && line.matches("^[A-Z\\s\\d\\p{Punct}]+$") && line.length() > 3) {
            return true;
        }

        return false;
    }

    private byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}
