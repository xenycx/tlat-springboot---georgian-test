package com.tlat.Repository;

import com.tlat.Entity.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
    StudentGroup findByCode(String code);
}
