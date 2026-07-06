package com.policyguardian.repository;

import com.policyguardian.model.Conversation;
import com.policyguardian.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUserOrderByStartedAtDesc(User user);
}
