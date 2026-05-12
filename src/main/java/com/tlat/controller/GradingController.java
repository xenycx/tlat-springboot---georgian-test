package com.tlat.controller;

import com.tlat.dto.AttendanceBatchDto;
import com.tlat.dto.GradingSummaryDto;
import com.tlat.entity.*;
import com.tlat.repository.LectureScheduleRepository;
import com.tlat.repository.StudentGroupRepository;
import com.tlat.service.AttendanceService;
import com.tlat.service.GradingService;
import com.tlat.service.UserService;
import com.tlat.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/grading")
@RequiredArgsConstructor
public class GradingController {

    private final GradingService gradingService;
    private final AttendanceService attendanceService;
    private final UserService userService;
    private final LectureScheduleRepository scheduleRepository;
    private final StudentGroupRepository groupRepository;
    private final SettingsService settingsService;


    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @GetMapping("/hub")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String showGradingHub(Model model, Principal principal) {
        User user = userService.findUserByEmail(principal.getName());
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
        
        List<Lecture> myLectures;
        if (isAdmin) {
            myLectures = scheduleRepository.findAll().stream().map(LectureSchedule::getLecture).distinct().collect(Collectors.toList());
        } else {
            myLectures = scheduleRepository.findAll().stream()
                .map(LectureSchedule::getLecture)
                .filter(l -> l != null && l.getLecturers().stream().anyMatch(lect -> lect.getId().equals(user.getId())))
                .distinct()
                .collect(Collectors.toList());
        }
        
        model.addAttribute("myLectures", myLectures);
        model.addAttribute("groups", groupRepository.findAll());
        
        return "grading/hub";
    }
    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @GetMapping("/attendance/{scheduleId}")
    public String showBatchAttendancePage(@PathVariable Long scheduleId, Model model) {
        LectureSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("განრიგი ვერ მოიძებნა"));

        List<User> eligibleStudents = attendanceService.getEligibleStudentsForSchedule(scheduleId);
        List<AttendanceRecord> existingRecords = attendanceService.getAttendanceForSchedule(scheduleId);
        
        Map<Long, AttendanceRecord> recordMap = existingRecords.stream()
                .collect(Collectors.toMap(r -> r.getStudent().getId(), r -> r));

        AttendanceBatchDto batchDto = new AttendanceBatchDto();
        batchDto.setScheduleId(scheduleId);
        List<AttendanceBatchDto.StudentAttendanceDto> studentDtos = new ArrayList<>();

        for (User student : eligibleStudents) {
            AttendanceBatchDto.StudentAttendanceDto dto = new AttendanceBatchDto.StudentAttendanceDto();
            dto.setStudentId(student.getId());
            
            AttendanceRecord record = recordMap.get(student.getId());
            if (record != null) {
                dto.setStatus(record.getStatus() != null ? record.getStatus() : AttendanceStatus.PRESENT);
                dto.setScore(record.getScore() != null ? record.getScore() : 0.0);
                dto.setNote(record.getNote());
            } else {
                dto.setStatus(AttendanceStatus.ABSENT); // Default to absent if no record exists when batch grading
                dto.setScore(0.0);
            }
            studentDtos.add(dto);
        }
        
        batchDto.setStudents(studentDtos);
        
        long maxAttendanceScore = settingsService.getLong(SettingsService.KEY_MAX_ATTENDANCE_SCORE, 30);
        long semesterWeeks = settingsService.getLong(SettingsService.KEY_SEMESTER_WEEKS, 13);
        int defaultMaxScorePerWeek = (int) Math.ceil((double) maxAttendanceScore / semesterWeeks);
        
        model.addAttribute("batchDto", batchDto);
        model.addAttribute("schedule", schedule);
        model.addAttribute("eligibleStudents", eligibleStudents);
        model.addAttribute("statuses", AttendanceStatus.values());
        model.addAttribute("maxScorePerWeek", defaultMaxScorePerWeek);

        return "grading/batch-attendance";
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/attendance/save")
    public String saveBatchAttendance(@ModelAttribute AttendanceBatchDto batchDto, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.findUserByEmail(principal.getName());
            gradingService.saveBatchAttendance(batchDto, currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "დასწრება და შეფასებები წარმატებით შეინახა.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "შეცდომა შენახვისას: " + e.getMessage());
        }
        return "redirect:/grading/attendance/" + batchDto.getScheduleId();
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @GetMapping("/ledger")
    public String showLedgerSelection(Model model, Principal principal) {
        // Here we could load all subjects/groups for the logged-in lecturer
        // For simplicity, we just show a form to select subject and group
        model.addAttribute("groups", groupRepository.findAll());
        return "grading/ledger-select";
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @GetMapping("/ledger/view")
    public String viewLedger(@RequestParam String subject, @RequestParam Long groupId, Model model) {
        List<GradingSummaryDto> grades = gradingService.getGradesForSubjectAndGroup(subject, groupId);
        StudentGroup group = groupRepository.findById(groupId).orElse(null);
        
        // Wrapper for form binding
        GradingSummaryForm form = new GradingSummaryForm();
        form.setSubject(subject);
        form.setGroupId(groupId);
        form.setGrades(grades);

        model.addAttribute("form", form);
        model.addAttribute("subject", subject);
        model.addAttribute("group", group);
        
        long maxAttendance = settingsService.getLong(SettingsService.KEY_MAX_ATTENDANCE_SCORE, 30);
        long maxMidterm = settingsService.getLong(SettingsService.KEY_MAX_MIDTERM_SCORE, 30);
        long maxFinal = settingsService.getLong(SettingsService.KEY_MAX_FINAL_SCORE, 40);
        
        model.addAttribute("maxAttendance", maxAttendance);
        model.addAttribute("maxMidterm", maxMidterm);
        model.addAttribute("maxFinal", maxFinal);
        
        return "grading/ledger-view";
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/ledger/save")
    public String saveLedger(@ModelAttribute GradingSummaryForm form, RedirectAttributes redirectAttributes) {
        try {
            gradingService.saveMidtermAndFinalScores(form.getSubject(), form.getGroupId(), form.getGrades());
            redirectAttributes.addFlashAttribute("successMessage", "ნიშნების უწყისი წარმატებით შეინახა.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "შეცდომა შენახვისას: " + e.getMessage());
        }
        return "redirect:/grading/ledger/view?subject=" + form.getSubject() + "&groupId=" + form.getGroupId();
    }

    // Inner class for form binding
    public static class GradingSummaryForm {
        private String subject;
        private Long groupId;
        private List<GradingSummaryDto> grades;

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
        public List<GradingSummaryDto> getGrades() { return grades; }
        public void setGrades(List<GradingSummaryDto> grades) { this.grades = grades; }
    }
}
