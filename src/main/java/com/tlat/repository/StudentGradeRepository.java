package com.tlat.repository;

import com.tlat.entity.StudentGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StudentGradeRepository extends JpaRepository<StudentGrade, Long> {
    List<StudentGrade> findBySubjectAndGroupId(String subject, Long groupId);
    Optional<StudentGrade> findByStudentIdAndSubjectAndGroupId(Long studentId, String subject, Long groupId);
    List<StudentGrade> findBySubject(String subject);
}
