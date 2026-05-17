package com.tlat.repository;

import com.tlat.entity.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
    StudentGroup findByCode(String code);
}
