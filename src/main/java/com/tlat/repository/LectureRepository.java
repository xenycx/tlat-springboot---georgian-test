package com.tlat.repository;

import com.tlat.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LectureRepository extends JpaRepository<Lecture, Long> {
    List<Lecture> findByLecturer(String lecturer);

    List<Lecture> findDistinctByLecturers_IdOrderByIdDesc(Long lecturerId);

    List<Lecture> findAllByOrderByIdDesc();

    List<Lecture> findByLecturerOrderByIdDesc(String lecturer);

    List<Lecture> findDistinctByGroups_IdOrderByIdDesc(Long groupId);

    long countByGroups_Id(Long groupId);

    boolean existsByIdAndLecturer(Long lectureId, String lecturer);

    boolean existsByIdAndLecturers_Id(Long lectureId, Long lecturerId);
}
