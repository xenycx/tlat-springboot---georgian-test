package com.tlat.service;

import com.tlat.dto.matrix.CreateRecordRequest;
import com.tlat.dto.matrix.CreateRecordResponse;
import com.tlat.dto.matrix.MatrixGradingDTO;
import com.tlat.dto.matrix.UpdateExamRequest;
import com.tlat.dto.matrix.UpdateScoreRequest;
import com.tlat.dto.matrix.UpdateScoreResponse;
import com.tlat.entity.*;
import com.tlat.repository.AttendanceRecordRepository;
import com.tlat.repository.LectureRepository;
import com.tlat.repository.LectureScheduleRepository;
import com.tlat.repository.StudentGroupRepository;
import com.tlat.repository.StudentGradeRepository;
import com.tlat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatrixGradingServiceImpl implements MatrixGradingService {

    private final LectureRepository lectureRepository;
    private final StudentGroupRepository groupRepository;
    private final LectureScheduleRepository scheduleRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final StudentGradeRepository studentGradeRepository;
    private final UserRepository userRepository;
    private final SettingsService settingsService;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public MatrixGradingDTO getMatrixData(Long lectureId, Long groupId, String username) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("Lecture not found"));
        
        StudentGroup group = null;
        if (groupId != null && groupId > 0) {
            group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        }

        User requestingUser = userService.findUserByEmail(username);
        boolean isAdmin = requestingUser.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        // Check ownership
        boolean isOwner = lecture.getLecturers().stream().anyMatch(l -> l.getId().equals(requestingUser.getId()));
        if (!isAdmin && !isOwner) {
            throw new SecurityException("Not authorized for this lecture");
        }

        int totalWeeks = (int) settingsService.getLong(SettingsService.KEY_SEMESTER_WEEKS, 13);
        int maxAttendanceScore = (int) settingsService.getLong(SettingsService.KEY_MAX_ATTENDANCE_SCORE, 30);
        int maxMidtermScore = (int) settingsService.getLong(SettingsService.KEY_MAX_MIDTERM_SCORE, 30);
        int maxFinalScore = (int) settingsService.getLong(SettingsService.KEY_MAX_FINAL_SCORE, 40);
        
        int baseWeekScore = maxAttendanceScore / totalWeeks;
        int remainder = maxAttendanceScore % totalWeeks;
        
        List<Integer> weeklyMaxScores = new ArrayList<>();
        for (int i = 0; i < totalWeeks; i++) {
            weeklyMaxScores.add(i < remainder ? baseWeekScore + 1 : baseWeekScore);
        }

        List<LectureSchedule> schedules = scheduleRepository.findByLecture_IdOrderByDateAscStartTimeAsc(lectureId);
        
        LocalDate semesterStartDate = schedules.isEmpty() ? LocalDate.now() : schedules.get(0).getDate();

        // Get all records for this lecture
        List<AttendanceRecord> records = attendanceRepository.findBySchedule_Lecture_Id(lectureId);
        
        List<User> students;
        List<StudentGrade> grades;
        if (group != null) {
            students = group.getStudents();
            grades = studentGradeRepository.findBySubjectAndGroupId(lecture.getSubject(), groupId);
        } else {
            students = lecture.getGroups().stream()
                    .flatMap(g -> g.getStudents().stream())
                    .distinct()
                    .collect(Collectors.toList());
            grades = studentGradeRepository.findBySubject(lecture.getSubject());
        }

        MatrixGradingDTO dto = new MatrixGradingDTO();
        MatrixGradingDTO.LectureDTO lDto = new MatrixGradingDTO.LectureDTO();
        lDto.setId(lecture.getId());
        lDto.setSubject(lecture.getSubject());
        dto.setLecture(lDto);

        if (group != null) {
            MatrixGradingDTO.GroupDTO gDto = new MatrixGradingDTO.GroupDTO();
            gDto.setId(group.getId());
            gDto.setCode(group.getCode());
            dto.setGroup(gDto);
        }

        dto.setSemesterStartDate(semesterStartDate);
        dto.setSemesterWeeks(totalWeeks);
        dto.setMaxMidtermScore(maxMidtermScore);
        dto.setMaxFinalScore(maxFinalScore);
        dto.setWeeklyMaxScores(weeklyMaxScores);

        // Build Columns
        List<MatrixGradingDTO.ColumnHeaderDTO> cols = new ArrayList<>();
        for (int i = 1; i <= totalWeeks; i++) {
            MatrixGradingDTO.ColumnHeaderDTO c = new MatrixGradingDTO.ColumnHeaderDTO();
            c.setWeek(i);
            c.setLabel("კვირა " + i);
            c.setWeekStart(semesterStartDate.plusWeeks(i - 1));
            c.setWeekEnd(semesterStartDate.plusWeeks(i).minusDays(1));
            cols.add(c);
        }
        dto.setColumns(cols);

        // Build Rows
        List<MatrixGradingDTO.StudentRowDTO> rows = new ArrayList<>();
        for (User student : students) {
            MatrixGradingDTO.StudentRowDTO row = new MatrixGradingDTO.StudentRowDTO();
            row.setStudentId(student.getId());
            row.setStudentName(student.getName());
            row.setStudentGroupName(student.getStudentGroup() != null ? student.getStudentGroup().getCode() : "უცნობი");
            row.setAvatarPath(student.getAvatarPath());

            Map<Integer, MatrixGradingDTO.StudentRowDTO.CellDTO> cells = new HashMap<>();
            
            // Filter records for this student
            List<AttendanceRecord> studentRecords = records.stream()
                    .filter(r -> r.getStudent().getId().equals(student.getId()))
                    .collect(Collectors.toList());

            // Find grade for this student
            Optional<StudentGrade> studentGradeOpt = grades.stream()
                    .filter(g -> g.getStudent().getId().equals(student.getId()))
                    .findFirst();
            
            Double midtermScore = studentGradeOpt.map(StudentGrade::getMidtermScore).orElse(null);
            Double finalScore = studentGradeOpt.map(StudentGrade::getFinalScore).orElse(null);

            // Group by week and aggregate
            Map<Integer, List<AttendanceRecord>> recordsByWeek = studentRecords.stream()
                    .collect(Collectors.groupingBy(r -> getWeekNumber(r.getSchedule().getDate(), semesterStartDate)));

            double totalAttendanceScore = 0;
            
            for (int w = 1; w <= totalWeeks; w++) {
                List<AttendanceRecord> weekRecords = recordsByWeek.getOrDefault(w, Collections.emptyList());
                
                if (!weekRecords.isEmpty()) {
                    MatrixGradingDTO.StudentRowDTO.CellDTO cell = new MatrixGradingDTO.StudentRowDTO.CellDTO();
                    double weekScore = weekRecords.stream().mapToDouble(r -> r.getScore() != null ? r.getScore() : 0.0).sum();
                    
                    // Assign latest schedule ID for updates
                    cell.setLectureScheduleId(weekRecords.get(weekRecords.size() - 1).getSchedule().getId());
                    cell.setScore(weekScore);
                    cell.setLetterGrade(getLetterGrade(weekScore));
                    cell.setWeek(w);
                    cells.put(w, cell);
                    
                    totalAttendanceScore += weekScore;
                }
            }
            
            row.setCells(cells);
            row.setTotalAttendanceScore(totalAttendanceScore);
            row.setMidtermScore(midtermScore);
            row.setFinalScore(finalScore);
            
            double totalScore = totalAttendanceScore + (midtermScore != null ? midtermScore : 0) + (finalScore != null ? finalScore : 0);
            row.setTotalScore(totalScore);
            row.setTotalGrade(getLetterGrade(totalScore));
            rows.add(row);
        }

        dto.setRows(rows);
        return dto;
    }

    @Override
    @Transactional
    public UpdateScoreResponse updateScore(UpdateScoreRequest request, String username) {
        AttendanceRecord record = attendanceRepository.findBySchedule_IdAndStudent_Id(
                request.getLectureScheduleId(), request.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Record not found"));

        // Verify ownership
        Lecture lecture = record.getSchedule().getLecture();
        User requestingUser = userService.findUserByEmail(username);
        boolean isAdmin = requestingUser.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
        
        boolean isOwner = lecture.getLecturers().stream().anyMatch(l -> l.getId().equals(requestingUser.getId()));
        if (!isAdmin && !isOwner) {
            throw new SecurityException("Not authorized");
        }

        // Enforce whole numbers
        double roundedScore = Math.round(request.getScore());
        
        // Ensure score does not exceed dynamic max limit
        int totalWeeks = (int) settingsService.getLong(SettingsService.KEY_SEMESTER_WEEKS, 13);
        int maxAttendanceScore = (int) settingsService.getLong(SettingsService.KEY_MAX_ATTENDANCE_SCORE, 30);
        int maxWeeklyScore = (int) Math.ceil((double) maxAttendanceScore / totalWeeks);
        if (roundedScore > maxWeeklyScore) {
            throw new IllegalArgumentException("Score exceeds maximum allowed for a single week (" + maxWeeklyScore + ")");
        }

        record.setScore(roundedScore);
        attendanceRepository.save(record);

        Long groupId = record.getStudent().getStudentGroup() != null ? record.getStudent().getStudentGroup().getId() : null;
        return calculateResponse(record.getStudent(), record.getSchedule().getDate(), lecture.getId(), groupId);
    }

    @Override
    @Transactional
    public CreateRecordResponse createRecord(CreateRecordRequest request, String username) {
        Lecture lecture = lectureRepository.findById(request.getLectureId())
                .orElseThrow(() -> new IllegalArgumentException("Lecture not found"));
        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        User requestingUser = userService.findUserByEmail(username);
        boolean isAdmin = requestingUser.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
        
        boolean isOwner = lecture.getLecturers().stream().anyMatch(l -> l.getId().equals(requestingUser.getId()));
        if (!isAdmin && !isOwner) {
            throw new SecurityException("Not authorized");
        }

        List<LectureSchedule> schedules = scheduleRepository.findByLecture_IdOrderByDateAscStartTimeAsc(lecture.getId());
        if (schedules.isEmpty()) {
            throw new IllegalArgumentException("No schedules for this lecture");
        }
        
        LocalDate semesterStartDate = schedules.get(0).getDate();
        
        // Find a schedule that belongs to the requested week
        LectureSchedule targetSchedule = schedules.stream()
                .filter(s -> getWeekNumber(s.getDate(), semesterStartDate) == request.getWeek())
                .findFirst()
                .orElse(null);
                
        // Fallback: take first schedule
        if (targetSchedule == null) {
            targetSchedule = schedules.get(0);
        }

        // Check if record already exists
        Optional<AttendanceRecord> existing = attendanceRepository.findBySchedule_IdAndStudent_Id(targetSchedule.getId(), student.getId());
        AttendanceRecord record;
        if (existing.isPresent()) {
            record = existing.get();
        } else {
            record = new AttendanceRecord();
            record.setSchedule(targetSchedule);
            record.setStudent(student);
            record.setStatus(AttendanceStatus.PRESENT);
            record.setAttendedAt(LocalDateTime.now());
        }
        
        // Enforce whole numbers
        double roundedScore = Math.round(request.getScore());
        
        // Ensure score does not exceed dynamic max limit
        int totalWeeks = (int) settingsService.getLong(SettingsService.KEY_SEMESTER_WEEKS, 13);
        int maxAttendanceScore = (int) settingsService.getLong(SettingsService.KEY_MAX_ATTENDANCE_SCORE, 30);
        int maxWeeklyScore = (int) Math.ceil((double) maxAttendanceScore / totalWeeks);
        if (roundedScore > maxWeeklyScore) {
            throw new IllegalArgumentException("Score exceeds maximum allowed for a single week (" + maxWeeklyScore + ")");
        }
        
        record.setScore(roundedScore);
        attendanceRepository.save(record);

        UpdateScoreResponse baseRes = calculateResponse(student, targetSchedule.getDate(), lecture.getId(), request.getGroupId());
        
        CreateRecordResponse res = new CreateRecordResponse();
        res.setSuccess(true);
        res.setRecordId(record.getId());
        res.setLetterGrade(baseRes.getLetterGrade());
        res.setNewTotal(baseRes.getNewTotal());
        res.setNewTotalGrade(baseRes.getNewTotalGrade());
        return res;
    }

    @Override
    @Transactional
    public UpdateScoreResponse updateExamScore(UpdateExamRequest request, String username) {
        Lecture lecture = lectureRepository.findById(request.getLectureId())
                .orElseThrow(() -> new IllegalArgumentException("Lecture not found"));
        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        StudentGroup group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        User requestingUser = userService.findUserByEmail(username);
        boolean isAdmin = requestingUser.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
        
        boolean isOwner = lecture.getLecturers().stream().anyMatch(l -> l.getId().equals(requestingUser.getId()));
        if (!isAdmin && !isOwner) {
            throw new SecurityException("Not authorized");
        }

        // Validate max score for exam type
        double roundedScore = Math.round(request.getScore());
        if ("MIDTERM".equals(request.getExamType())) {
            int maxMidtermScore = (int) settingsService.getLong(SettingsService.KEY_MAX_MIDTERM_SCORE, 30);
            if (roundedScore > maxMidtermScore) {
                throw new IllegalArgumentException("Score exceeds maximum for midterm (" + maxMidtermScore + ")");
            }
        } else if ("FINAL".equals(request.getExamType())) {
            int maxFinalScore = (int) settingsService.getLong(SettingsService.KEY_MAX_FINAL_SCORE, 40);
            if (roundedScore > maxFinalScore) {
                throw new IllegalArgumentException("Score exceeds maximum for final exam (" + maxFinalScore + ")");
            }
        } else {
            throw new IllegalArgumentException("Invalid exam type");
        }

        // Find or create StudentGrade
        StudentGrade grade = studentGradeRepository.findByStudentIdAndSubjectAndGroupId(
                student.getId(), lecture.getSubject(), group.getId())
                .orElseGet(() -> {
                    StudentGrade newGrade = new StudentGrade();
                    newGrade.setStudent(student);
                    newGrade.setSubject(lecture.getSubject());
                    newGrade.setGroup(group);
                    newGrade.setMidtermScore(0.0);
                    newGrade.setFinalScore(0.0);
                    return newGrade;
                });

        if ("MIDTERM".equals(request.getExamType())) {
            grade.setMidtermScore(roundedScore);
        } else {
            grade.setFinalScore(roundedScore);
        }
        
        studentGradeRepository.save(grade);

        return calculateResponse(student, null, lecture.getId(), group.getId());
    }

    private int getWeekNumber(LocalDate date, LocalDate startDate) {
        long days = ChronoUnit.DAYS.between(startDate, date);
        return (int) (days / 7) + 1;
    }

    private String getLetterGrade(double score) {
        if (score >= 91) return "A";
        if (score >= 81) return "B";
        if (score >= 71) return "C";
        if (score >= 61) return "D";
        if (score >= 51) return "E";
        return "F";
    }

    private UpdateScoreResponse calculateResponse(User student, LocalDate scheduleDate, Long lectureId, Long groupId) {
        List<LectureSchedule> schedules = scheduleRepository.findByLecture_IdOrderByDateAscStartTimeAsc(lectureId);
        LocalDate startDate = schedules.isEmpty() ? LocalDate.now() : schedules.get(0).getDate();
        
        int targetWeek = scheduleDate != null ? getWeekNumber(scheduleDate, startDate) : -1;
        
        List<AttendanceRecord> records = attendanceRepository.findBySchedule_Lecture_Id(lectureId).stream()
                .filter(r -> r.getStudent().getId().equals(student.getId()))
                .collect(Collectors.toList());
                
        double weekScore = 0;
        if (targetWeek != -1) {
            weekScore = records.stream()
                    .filter(r -> getWeekNumber(r.getSchedule().getDate(), startDate) == targetWeek)
                    .mapToDouble(r -> r.getScore() != null ? r.getScore() : 0.0)
                    .sum();
        }
                
        double totalAttendanceScore = records.stream()
                .mapToDouble(r -> r.getScore() != null ? r.getScore() : 0.0)
                .sum();

        Lecture lecture = lectureRepository.findById(lectureId).orElse(null);
        Optional<StudentGrade> studentGradeOpt = studentGradeRepository.findByStudentIdAndSubjectAndGroupId(
                student.getId(), lecture != null ? lecture.getSubject() : "", groupId);
        
        double midtermScore = studentGradeOpt.map(StudentGrade::getMidtermScore).orElse(0.0);
        double finalScore = studentGradeOpt.map(StudentGrade::getFinalScore).orElse(0.0);

        double totalScore = totalAttendanceScore + midtermScore + finalScore;

        UpdateScoreResponse res = new UpdateScoreResponse();
        res.setSuccess(true);
        if (targetWeek != -1) {
            res.setLetterGrade(getLetterGrade(weekScore));
        }
        res.setNewTotalAttendance(totalAttendanceScore);
        res.setNewTotal(totalScore);
        res.setNewTotalGrade(getLetterGrade(totalScore));
        return res;
    }
}