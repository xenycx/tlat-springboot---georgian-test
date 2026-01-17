package com.tlat.Repository;

import java.time.LocalDate;
import java.util.List;
import com.tlat.Entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureRepository extends JpaRepository<Lecture, Long> {
    List<Lecture> findByLecturer(String lecturer);
    List<Lecture> findByDate(LocalDate date);
    List<Lecture> findByDateAndLecturer(LocalDate date, String lecturer);
    
    // Sorted queries - newest first
    List<Lecture> findAllByOrderByIdDesc();
    List<Lecture> findByLecturerOrderByIdDesc(String lecturer);
    List<Lecture> findByDateOrderByStartTimeDesc(LocalDate date);
    List<Lecture> findByDateAndLecturerOrderByStartTimeDesc(LocalDate date, String lecturer);
}