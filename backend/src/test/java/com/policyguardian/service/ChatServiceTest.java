package com.policyguardian.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.policyguardian.dto.ChatResponse;
import com.policyguardian.dto.ChromaSearchResult;
import com.policyguardian.model.Conversation;
import com.policyguardian.model.Role;
import com.policyguardian.model.User;
import com.policyguardian.repository.ConversationRepository;
import com.policyguardian.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private GroqService groqService;

    @Mock
    private ChromaService chromaService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ChatService chatService;

    private User testUser;
    private Conversation mockConversation;

    @BeforeEach
    public void setup() {
        testUser = User.builder()
                .id(1L)
                .username("teststudent")
                .email("student@test.com")
                .password("hash")
                .role(Role.STUDENT)
                .department("Computer Science")
                .build();

        mockConversation = Conversation.builder()
                .id(42L)
                .user(testUser)
                .build();
    }

    @Test
    public void testProcessQuerySuccess() {
        // Mocking conversation resolution
        when(conversationRepository.findById(eq(42L))).thenReturn(Optional.of(mockConversation));

        // Mocking message history
        when(messageRepository.findTop10ByConversationIdOrderByCreatedAtDesc(eq(42L)))
                .thenReturn(List.of());

        // Mocking embedding vector
        List<Double> mockEmbedding = List.of(0.1, 0.2, 0.3);
        when(groqService.getEmbedding(anyString())).thenReturn(mockEmbedding);

        // Mocking ChromaDB search
        when(chromaService.getOrCreateCollection(anyString())).thenReturn("collection-uuid");
        
        List<ChromaSearchResult> searchResults = List.of(
                new ChromaSearchResult(
                        "chunk_1",
                        "Students are allowed 5 sick leaves per semester.",
                        0.05,
                        1L,
                        "LeavePolicy.pdf",
                        "Section 2: Health Leaves",
                        "STUDENT",
                        "ALL"
                )
        );
        when(chromaService.queryCollection(
                eq("collection-uuid"),
                eq(mockEmbedding),
                eq(10),
                eq("STUDENT"),
                eq("Computer Science"),
                anyString()
        )).thenReturn(searchResults);

        // Mocking Groq text response with structured JSON
        String mockedGroqJson = """
                {
                  "answer": "Students can take up to 5 sick leaves every semester.",
                  "citations": [
                    {
                      "doc_id": "1",
                      "doc_name": "LeavePolicy.pdf",
                      "section": "Section 2: Health Leaves",
                      "excerpt": "Students are allowed 5 sick leaves per semester."
                    }
                  ],
                  "confidence": "high",
                  "insufficient_context": false,
                  "escalate": false,
                  "escalation_contact": null,
                  "follow_up_suggestions": []
                }
                """;
        when(groqService.generateContent(anyString(), anyString())).thenReturn(mockedGroqJson);

        // Execute test
        ChatResponse response = chatService.processQuery("What is the sick leave policy?", "42", testUser);

        // Assertions
        assertNotNull(response);
        assertEquals("Students can take up to 5 sick leaves every semester.", response.answer());
        assertEquals("high", response.confidence());
        assertFalse(response.escalate());
        assertEquals("42", response.sessionId());
        assertEquals(1, response.citations().size());
        assertEquals("LeavePolicy.pdf", response.citations().get(0).docName());

        // Verify that messages were stored in MySQL (one USER question, one ASSISTANT answer)
        verify(messageRepository, times(2)).save(any());
    }
}
