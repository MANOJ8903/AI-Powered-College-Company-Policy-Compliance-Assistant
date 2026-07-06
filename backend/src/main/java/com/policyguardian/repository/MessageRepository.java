package com.policyguardian.repository;

import com.policyguardian.model.Conversation;
import com.policyguardian.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);

    // Retrieve last N turns of a conversation for chat memory
    List<Message> findTop10ByConversationIdOrderByCreatedAtDesc(Long conversationId);

    // Retrieve escalated questions for review
    List<Message> findByEscalateFlagTrueOrderByCreatedAtDesc();

    // Query count grouped by department of the querying user (only count user queries)
    @Query("SELECT m.conversation.user.department, COUNT(m) FROM Message m WHERE m.sender = 'USER' GROUP BY m.conversation.user.department")
    List<Object[]> countQueriesByDepartment();

    // Unanswered questions: find USER questions where the ASSISTANT's response had insufficient context
    @Query("SELECT m.content, COUNT(m) FROM Message m WHERE m.sender = 'USER' AND EXISTS (SELECT a FROM Message a WHERE a.conversation = m.conversation AND a.sender = 'ASSISTANT' AND a.insufficientContext = true AND a.createdAt >= m.createdAt) GROUP BY m.content ORDER BY COUNT(m) DESC")
    List<Object[]> findMostFrequentUnansweredQuestions();

    // Usage over time: query count grouped by creation date, user department, and user role
    @Query("SELECT CAST(m.createdAt AS date), m.conversation.user.department, m.conversation.user.role, COUNT(m) FROM Message m WHERE m.sender = 'USER' GROUP BY CAST(m.createdAt AS date), m.conversation.user.department, m.conversation.user.role ORDER BY CAST(m.createdAt AS date) ASC")
    List<Object[]> findUsageOverTime();
}
