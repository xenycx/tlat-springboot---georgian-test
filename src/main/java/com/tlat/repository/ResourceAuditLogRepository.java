package com.tlat.repository;

import com.tlat.entity.ResourceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceAuditLogRepository extends JpaRepository<ResourceAuditLog, Long> {

    List<ResourceAuditLog> findTop200ByOrderByCreatedAtDesc();
}
