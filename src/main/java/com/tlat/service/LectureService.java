package com.tlat.service;

import com.tlat.Dto.LectureDto;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;

public interface LectureService {
    void saveLecture(LectureDto lectureDto);
    List<LectureDto> findAllLectures();
    List<LectureDto> findLecturesByLecturerId(Long lecturerId);
    List<LectureDto> findLecturesByDate(LocalDate date);
    List<LectureDto> findLecturesByDateAndLecturerId(LocalDate date, Long lecturerId);
    List<LectureDto> findLecturesByGroupId(Long groupId);
    List<LectureDto> findLecturesByDateAndGroupId(LocalDate date, Long groupId);
    List<LectureDto> findUpcomingLecturesByGroupId(Long groupId, LocalDate date);
    LectureDto findLectureById(Long id);
    void editLecture(LectureDto lectureDto, Long id);
    void deleteLectureById(Long id);
    void addSchedule(Long lectureId, LectureDto scheduleDto);
    void deleteScheduleById(Long scheduleId);
    List<LectureDto> findSchedulesByLectureId(Long lectureId);
    void importLecturesFromCsv(MultipartFile file);
    void startLecture(Long id, HttpServletRequest request);
    void stopLecture(Long id, HttpServletRequest request);
    boolean canLecturerManageSchedule(Long scheduleId, Long lecturerId);
    boolean canLecturerManageLecture(Long lectureId, Long lecturerId);
    
}
