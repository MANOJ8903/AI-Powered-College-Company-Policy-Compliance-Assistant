package com.policyguardian.controller;

import com.policyguardian.model.Message;
import com.policyguardian.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
public class AnalyticsController {

    @Autowired
    private MessageRepository messageRepository;

    @GetMapping("/queries-by-dept")
    public ResponseEntity<List<Object[]>> getQueriesByDept() {
        List<Object[]> stats = messageRepository.countQueriesByDepartment();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/escalated")
    public ResponseEntity<List<Message>> getEscalatedQueries() {
        List<Message> escalations = messageRepository.findByEscalateFlagTrueOrderByCreatedAtDesc();
        return ResponseEntity.ok(escalations);
    }
}
