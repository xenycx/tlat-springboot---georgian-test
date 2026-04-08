package com.tlat.Controller;

import com.tlat.Entity.AttendanceRecord;
import com.tlat.Entity.LectureSchedule;
import com.tlat.Entity.User;
import com.tlat.Repository.LectureScheduleRepository;
import com.tlat.service.AttendanceService;
import com.tlat.service.UserService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserService userService;
    private final LectureScheduleRepository lectureScheduleRepository;

    @Autowired
    public AttendanceController(AttendanceService attendanceService,
                                 UserService userService,
                                 LectureScheduleRepository lectureScheduleRepository) {
        this.attendanceService = attendanceService;
        this.userService = userService;
        this.lectureScheduleRepository = lectureScheduleRepository;
    }

    @GetMapping("/checkin/{token}")
    public String showCheckinPage(@PathVariable String token, Model model, Principal principal) {
        if (!attendanceService.isTokenValid(token)) {
            model.addAttribute("error", "ბმული არასწორია ან ვადა ამოიწურა");
            return "attendance/checkin";
        }

        LectureSchedule schedule = attendanceService.getScheduleByToken(token);
        model.addAttribute("token", token);
        model.addAttribute("schedule", schedule);

        if (principal != null) {
            User user = userService.findUserByEmail(principal.getName());
            model.addAttribute("currentUser", user);
            boolean isStudent = user.getRoles().stream()
                    .anyMatch(r -> "ROLE_STUDENT".equals(r.getName()));
            model.addAttribute("isStudent", isStudent);

            if (isStudent && schedule != null) {
                boolean alreadyMarked = attendanceService.getAttendanceForSchedule(schedule.getId())
                        .stream().anyMatch(a -> a.getStudent().getId().equals(user.getId()));
                model.addAttribute("alreadyMarked", alreadyMarked);
            }
        }

        if (schedule != null) {
            model.addAttribute("eligibleStudents", attendanceService.getEligibleStudentsForSchedule(schedule.getId()));
        }

        return "attendance/checkin";
    }

    @PostMapping("/checkin/{token}/authenticated")
    public String checkinAuthenticated(@PathVariable String token,
                                        Principal principal,
                                        HttpServletRequest request,
                                        Model model) {
        try {
            if (principal == null) {
                model.addAttribute("error", "გთხოვთ გაიაროთ ავტორიზაცია");
                return showCheckinPage(token, model, null);
            }
            User user = userService.findUserByEmail(principal.getName());
            attendanceService.markAttendanceAuthenticated(token, user, request);
            model.addAttribute("success", "დასწრება წარმატებით აღინიშნა!");
            model.addAttribute("alreadyMarked", true);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        populateCheckinModel(token, model, principal);
        return "attendance/checkin";
    }

    @PostMapping("/checkin/{token}/identify")
    public String checkinSelfIdentified(@PathVariable String token,
                                         @RequestParam Long studentId,
                                         HttpServletRequest request,
                                         Model model,
                                         Principal principal) {
        try {
            attendanceService.markAttendanceSelfIdentified(token, studentId, request);
            model.addAttribute("success", "დასწრება აღინიშნა (საჭიროებს დადასტურებას)");
            model.addAttribute("alreadyMarked", true);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        populateCheckinModel(token, model, principal);
        return "attendance/checkin";
    }

    @GetMapping(value = "/qr-image/{scheduleId}", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> generateQrCode(@PathVariable Long scheduleId, HttpServletRequest request) {
        try {
            LectureSchedule schedule = lectureScheduleRepository.findById(scheduleId)
                    .orElse(null);
            if (schedule == null || schedule.getAttendanceToken() == null) {
                return ResponseEntity.notFound().build();
            }

            String baseUrl = request.getScheme() + "://" + request.getServerName();
            int port = request.getServerPort();
            if ((request.getScheme().equals("http") && port != 80) ||
                (request.getScheme().equals("https") && port != 443)) {
                baseUrl += ":" + port;
            }

            String checkinUrl = baseUrl + "/attendance/checkin/" + schedule.getAttendanceToken();

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(checkinUrl, BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/manual/{scheduleId}")
    public String manualAdd(@PathVariable Long scheduleId,
                             @RequestParam Long studentId,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            User lecturer = userService.findUserByEmail(principal.getName());
            attendanceService.markAttendanceManual(scheduleId, studentId, lecturer);
            redirectAttributes.addFlashAttribute("successMessage", "სტუდენტი დაემატა");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/attendance/review/" + scheduleId;
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/regenerate/{scheduleId}")
    public String regenerateToken(@PathVariable Long scheduleId,
                                   RedirectAttributes redirectAttributes) {
        try {
            attendanceService.generateAttendanceToken(scheduleId);
            redirectAttributes.addFlashAttribute("successMessage", "QR კოდი განახლდა");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/attendance/review/" + scheduleId;
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @GetMapping("/review/{scheduleId}")
    public String reviewAttendance(@PathVariable Long scheduleId, Model model, Principal principal) {
        LectureSchedule schedule = lectureScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("განრიგი ვერ მოიძებნა"));

        User user = userService.findUserByEmail(principal.getName());
        List<AttendanceRecord> records = attendanceService.getAttendanceForSchedule(scheduleId);
        List<User> eligibleStudents = attendanceService.getEligibleStudentsForSchedule(scheduleId);

        model.addAttribute("user", user);
        model.addAttribute("schedule", schedule);
        model.addAttribute("lecture", schedule.getLecture());
        model.addAttribute("records", records);
        model.addAttribute("eligibleStudents", eligibleStudents);
        model.addAttribute("attendanceCount", attendanceService.getAttendanceCount(scheduleId));
        model.addAttribute("totalStudents", eligibleStudents.size());

        return "attendance/review";
    }

    @GetMapping("/count/{scheduleId}")
    @ResponseBody
    public ResponseEntity<Long> getAttendanceCount(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(attendanceService.getAttendanceCount(scheduleId));
    }

    private void populateCheckinModel(String token, Model model, Principal principal) {
        LectureSchedule schedule = attendanceService.getScheduleByToken(token);
        model.addAttribute("token", token);
        model.addAttribute("schedule", schedule);
        if (principal != null) {
            User user = userService.findUserByEmail(principal.getName());
            model.addAttribute("currentUser", user);
            model.addAttribute("isStudent", user.getRoles().stream()
                    .anyMatch(r -> "ROLE_STUDENT".equals(r.getName())));
        }
        if (schedule != null) {
            model.addAttribute("eligibleStudents",
                    attendanceService.getEligibleStudentsForSchedule(schedule.getId()));
        }
    }
}
