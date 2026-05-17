package com.tlat.repository;

import com.tlat.entity.LectureSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LectureScheduleRepository extends JpaRepository<LectureSchedule, Long> {

    List<LectureSchedule> findByDateOrderByStartTimeDesc(LocalDate date);

    List<LectureSchedule> findByLecture_LecturerAndDateOrderByStartTimeAsc(String lecturer, LocalDate date);

    List<LectureSchedule> findDistinctByLecture_Lecturers_IdAndDateOrderByStartTimeAsc(Long lecturerId, LocalDate date);

    List<LectureSchedule> findByLecture_Groups_IdAndDateOrderByStartTimeAsc(Long groupId, LocalDate date);

    List<LectureSchedule> findByLecture_Groups_IdAndDateGreaterThanOrderByDateAscStartTimeAsc(Long groupId, LocalDate date);

    List<LectureSchedule> findByLecture_IdOrderByDateAscStartTimeAsc(Long lectureId);

    List<LectureSchedule> findByLecture_IdOrderByDateDescStartTimeDesc(Long lectureId);

    boolean existsByIdAndLecture_Lecturer(Long scheduleId, String lecturer);

    boolean existsByIdAndLecture_Lecturers_Id(Long scheduleId, Long lecturerId);

    Optional<LectureSchedule> findByAttendanceToken(String attendanceToken);
}
