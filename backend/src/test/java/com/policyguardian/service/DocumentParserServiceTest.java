package com.policyguardian.service;

import com.policyguardian.dto.TextChunk;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class DocumentParserServiceTest {

    private final DocumentParserService parserService = new DocumentParserService();

    @Test
    public void testChunkTextSimple() {
        String text = "This is a simple text that we want to chunk. It does not contain any headers initially.";
        List<TextChunk> chunks = parserService.chunkText(text, 30, 5);

        assertFalse(chunks.isEmpty());
        for (TextChunk chunk : chunks) {
            assertTrue(chunk.content().length() <= 30);
            assertEquals("General / Introduction", chunk.sectionName());
        }
    }

    @Test
    public void testChunkTextWithSections() {
        String text = """
                General intro text here.
                SECTION 1: Leave Policy
                Employee leaves are capped at 30 days per calendar year.
                Rule 5: Attendance
                Daily check-ins must be completed by 10 AM.
                """;

        List<TextChunk> chunks = parserService.chunkText(text, 100, 10);

        assertFalse(chunks.isEmpty());
        
        // Let's verify that section names are parsed and associated correctly
        boolean foundSection1 = false;
        boolean foundRule5 = false;
        
        for (TextChunk chunk : chunks) {
            if (chunk.sectionName().equals("SECTION 1: Leave Policy")) {
                foundSection1 = true;
                assertTrue(chunk.content().contains("capped at 30 days"));
            }
            if (chunk.sectionName().equals("Rule 5: Attendance")) {
                foundRule5 = true;
                assertTrue(chunk.content().contains("Daily check-ins"));
            }
        }
        
        assertTrue(foundSection1, "Should have parsed SECTION 1: Leave Policy");
        assertTrue(foundRule5, "Should have parsed Rule 5: Attendance");
    }
}
