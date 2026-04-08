package com.tlat.Repository;

import com.tlat.Entity.ResourceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceAuditLogRepository extends JpaRepository<ResourceAuditLog, Long> {

    List<ResourceAuditLog> findTop200ByOrderByCreatedAtDesc();
}
