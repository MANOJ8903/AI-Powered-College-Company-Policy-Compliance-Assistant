package com.policyguardian.repository;

import com.policyguardian.model.PolicyDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, Long> {
    List<PolicyDocument> findByDepartmentScopeOrDepartmentScope(String scope1, String scope2);
}
