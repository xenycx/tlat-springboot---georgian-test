package com.tlat.Controller;

import com.tlat.Dto.LectureDto;
import com.tlat.Entity.User;
import com.tlat.service.LectureService;
import com.tlat.service.PdfExportService;
import com.tlat.service.RoomService;
import com.tlat.service.StudentGroupService;
import com.tlat.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/lectures")
public class LectureController {

    private final LectureService lectureService;
    private final RoomService roomService;
    private final UserService userService;
    private final StudentGroupService studentGroupService;
    private final PdfExportService pdfExportService;

    @Autowired
    public LectureController(
            LectureService lectureService,
            RoomService roomService,
            UserService userService,
            StudentGroupService studentGroupService,
            PdfExportService pdfExportService) {
        this.lectureService = lectureService;
        this.roomService = roomService;
        this.userService = userService;
        this.studentGroupService = studentGroupService;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping
    public String listLectures(Model model, Principal principal) {
        User user = userService.findUserByEmail(principal.getName());
        List<LectureDto> lectures = new ArrayList<>();

        try {
            lectures = getAccessibleLectures(user);
        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("lectures", lectures != null ? lectures : new ArrayList<>());
        model.addAttribute("user", user);
        return "lecture/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("lecture", new LectureDto());
        model.addAttribute("rooms", roomService.findAllRooms());
        model.addAttribute("users", userService.findUsersByRole("ROLE_LECTURER"));
        model.addAttribute("groups", studentGroupService.findAllGroups());
        return "lecture/add";
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/add")
    public String addLecture(@jakarta.validation.Valid @ModelAttribute("lecture") LectureDto lecture,
                             org.springframework.validation.BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (lecture.getLecturerIds() == null || lecture.getLecturerIds().isEmpty()) {
            result.rejectValue("lecturerIds", "", "ლექციას მინიმუმ ერთი ლექტორი უნდა ჰქონდეს მინიჭებული");
        }
        if (lecture.getGroupIds() == null || lecture.getGroupIds().isEmpty()) {
            result.rejectValue("groupIds", "", "ლექციას მინიმუმ ერთი ჯგუფი უნდა ჰქონდეს მინიჭებული");
        }
        if (result.hasErrors()) {
            model.addAttribute("rooms", roomService.findAllRooms());
            model.addAttribute("users", userService.findUsersByRole("ROLE_LECTURER"));
            model.addAttribute("groups", studentGroupService.findAllGroups());
            return "lecture/add";
        }
        try {
            lectureService.saveLecture(lecture);
            redirectAttributes.addFlashAttribute("successMessage", "ლექცია წარმატებით დაემატა");
            return "redirect:/lectures";
        } catch (RuntimeException e) {
            model.addAttribute("rooms", roomService.findAllRooms());
            model.addAttribute("users", userService.findUsersByRole("ROLE_LECTURER"));
            model.addAttribute("groups", studentGroupService.findAllGroups());
            model.addAttribute("errorMessage", e.getMessage());
            return "lecture/add";
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/import")
    public String importCsv(@RequestParam("file") MultipartFile file) {
        lectureService.importLecturesFromCsv(file);
        return "redirect:/lectures";
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, Principal principal) {
        User user = userService.findUserByEmail(principal.getName());
        validateLecturePermission(id, user);
        LectureDto lecture = lectureService.findLectureById(id);
        model.addAttribute("lecture", lecture);
        model.addAttribute("schedules", lectureService.findSchedulesByLectureId(id));
        model.addAttribute("newSchedule", new LectureDto());
        model.addAttribute("rooms", roomService.findAllRooms());
        model.addAttribute("users", userService.findUsersByRole("ROLE_LECTURER"));
        model.addAttribute("groups", studentGroupService.findAllGroups());
        return "lecture/edit";
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/edit/{id}")
    public String editLecture(@jakarta.validation.Valid @ModelAttribute("lecture") LectureDto lecture,
                              org.springframework.validation.BindingResult result,
                              @PathVariable Long id,
                              Model model,
                              Principal principal) {
        User user = userService.findUserByEmail(principal.getName());
        validateLecturePermission(id, user);
        if (lecture.getLecturerIds() == null || lecture.getLecturerIds().isEmpty()) {
            result.rejectValue("lecturerIds", "", "ლექციას მინიმუმ ერთი ლექტორი უნდა ჰქონდეს მინიჭებული");
        }
        if (lecture.getGroupIds() == null || lecture.getGroupIds().isEmpty()) {
            result.rejectValue("groupIds", "", "ლექციას მინიმუმ ერთი ჯგუფი უნდა ჰქონდეს მინიჭებული");
        }
        if (result.hasErrors()) {
            model.addAttribute("schedules", lectureService.findSchedulesByLectureId(id));
            model.addAttribute("newSchedule", new LectureDto());
            model.addAttribute("rooms", roomService.findAllRooms());
            model.addAttribute("users", userService.findUsersByRole("ROLE_LECTURER"));
            model.addAttribute("groups", studentGroupService.findAllGroups());
            return "lecture/edit";
        }
        try {
            lectureService.editLecture(lecture, id);
            return "redirect:/lectures";
        } catch (RuntimeException e) {
            model.addAttribute("schedules", lectureService.findSchedulesByLectureId(id));
            model.addAttribute("newSchedule", new LectureDto());
            model.addAttribute("rooms", roomService.findAllRooms());
            model.addAttribute("users", userService.findUsersByRole("ROLE_LECTURER"));
            model.addAttribute("groups", studentGroupService.findAllGroups());
            model.addAttribute("errorMessage", e.getMessage());
            return "lecture/edit";
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/{id}/schedules/add")
    public String addSchedule(@PathVariable Long id,
                              @ModelAttribute("newSchedule") LectureDto schedule,
                              RedirectAttributes redirectAttributes,
                              Principal principal) {
        try {
            User user = userService.findUserByEmail(principal.getName());
            validateLecturePermission(id, user);
            lectureService.addSchedule(id, schedule);
            redirectAttributes.addFlashAttribute("successMessage", "განრიგი დაემატა");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/lectures/edit/" + id;
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @GetMapping("/{lectureId}/schedules/delete/{scheduleId}")
    public String deleteSchedule(@PathVariable Long lectureId,
                                 @PathVariable Long scheduleId,
                                 RedirectAttributes redirectAttributes,
                                 Principal principal) {
        try {
            User user = userService.findUserByEmail(principal.getName());
            if (!hasRole(user, "ROLE_ADMIN") && !lectureService.canLecturerManageSchedule(scheduleId, user.getId())) {
                throw new RuntimeException("თქვენ არ გაქვთ ამ განრიგის წაშლის უფლება");
            }
            lectureService.deleteScheduleById(scheduleId);
            redirectAttributes.addFlashAttribute("successMessage", "განრიგი წაიშალა");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/lectures/edit/" + lectureId;
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @GetMapping("/delete/{id}")
    public String deleteLecture(@PathVariable Long id, Principal principal) {
        User user = userService.findUserByEmail(principal.getName());
        validateLecturePermission(id, user);
        lectureService.deleteLectureById(id);
        return "redirect:/lectures";
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/start/{id}")
    public String startLecture(@PathVariable Long id,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes,
                               Principal principal,
                               @RequestHeader(value = "Referer", required = false) String referer) {
        try {
            User user = userService.findUserByEmail(principal.getName());
            if (!hasRole(user, "ROLE_ADMIN") && !lectureService.canLecturerManageSchedule(id, user.getId())) {
                throw new RuntimeException("თქვენ არ გაქვთ ამ ლექციის დაწყების უფლება");
            }
            lectureService.startLecture(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "ლექცია წარმატებით დაიწყო");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return referer != null ? "redirect:" + referer : "redirect:/lectures";
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/stop/{id}")
    public String stopLecture(@PathVariable Long id,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes,
                              Principal principal,
                              @RequestHeader(value = "Referer", required = false) String referer) {
        try {
            User user = userService.findUserByEmail(principal.getName());
            if (!hasRole(user, "ROLE_ADMIN") && !lectureService.canLecturerManageSchedule(id, user.getId())) {
                throw new RuntimeException("თქვენ არ გაქვთ ამ ლექციის დასრულების უფლება");
            }
            lectureService.stopLecture(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "ლექცია წარმატებით დასრულდა");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return referer != null ? "redirect:" + referer : "redirect:/lectures";
    }

    @GetMapping("/export/pdf/{id}")
    public ResponseEntity<byte[]> exportLecturePdf(@PathVariable Long id) {
        try {
            LectureDto lecture = lectureService.findLectureById(id);
            ByteArrayOutputStream pdfStream = pdfExportService.generateLecturePdf(lecture);

            String filename = "lecture_" + id + "_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfStream.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/pdf/all")
    public ResponseEntity<byte[]> exportAllLecturesPdf(Principal principal) {
        try {
            User user = userService.findUserByEmail(principal.getName());
            List<LectureDto> lectures = getAccessibleLectures(user);

            ByteArrayOutputStream pdfStream = pdfExportService.generateLecturesPdf(
                    lectures, "ყველა ლექცია");

            String filename = "all_lectures_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfStream.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/excel/all")
    public ResponseEntity<byte[]> exportAllLecturesExcel(Principal principal) {
        try {
            User user = userService.findUserByEmail(principal.getName());
            List<LectureDto> lectures = getAccessibleLectures(user);

            ByteArrayOutputStream excelStream = pdfExportService.generateLecturesExcel(lectures);

            String filename = "all_lectures_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelStream.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/pdf/filtered")
    public ResponseEntity<byte[]> exportFilteredLecturesPdf(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status,
            Principal principal) {
        try {
            User user = userService.findUserByEmail(principal.getName());
            List<LectureDto> allLectures = getAccessibleLectures(user);

            List<LectureDto> filteredLectures = allLectures.stream()
                    .filter(lecture -> {
                        boolean matches = true;

                        if (lecture.getDate() == null) {
                            return false;
                        }

                        if (startDate != null && !startDate.isEmpty()) {
                            LocalDate start = LocalDate.parse(startDate);
                            matches = !lecture.getDate().isBefore(start);
                        }

                        if (matches && endDate != null && !endDate.isEmpty()) {
                            LocalDate end = LocalDate.parse(endDate);
                            matches = !lecture.getDate().isAfter(end);
                        }

                        if (matches && status != null && !status.isEmpty() && !status.equals("ALL") && lecture.getStatus() != null) {
                            matches = lecture.getStatus().toString().equals(status);
                        }

                        return matches;
                    })
                    .toList();

            String title = "Filtered Lectures Report";
            if (startDate != null && endDate != null) {
                title += " (" + startDate + " to " + endDate + ")";
            }

            ByteArrayOutputStream pdfStream = pdfExportService.generateLecturesPdf(
                    filteredLectures, title);

            String filename = "filtered_lectures_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfStream.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/pdf/today")
    public ResponseEntity<byte[]> exportTodayLecturesPdf(Principal principal) {
        try {
            User user = userService.findUserByEmail(principal.getName());
            List<LectureDto> todaysLectures = getAccessibleTodaysLectures(user);

            String title = "დღევანდელი ლექციები - " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            ByteArrayOutputStream pdfStream = pdfExportService.generateLecturesPdf(todaysLectures, title);

            String filename = "todays_lectures_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfStream.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/excel/today")
    public ResponseEntity<byte[]> exportTodayLecturesExcel(Principal principal) {
        try {
            User user = userService.findUserByEmail(principal.getName());
            List<LectureDto> todaysLectures = getAccessibleTodaysLectures(user);

            ByteArrayOutputStream excelStream = pdfExportService.generateLecturesExcel(todaysLectures);

            String filename = "todays_lectures_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelStream.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private List<LectureDto> getAccessibleLectures(User user) {
        if (hasRole(user, "ROLE_ADMIN")) {
            return lectureService.findAllLectures();
        }
        if (hasRole(user, "ROLE_STUDENT")) {
            if (user.getStudentGroup() == null) {
                return new ArrayList<>();
            }
            return lectureService.findLecturesByGroupId(user.getStudentGroup().getId());
        }
        return lectureService.findLecturesByLecturerId(user.getId());
    }

    private List<LectureDto> getAccessibleTodaysLectures(User user) {
        if (hasRole(user, "ROLE_ADMIN")) {
            return lectureService.findLecturesByDate(LocalDate.now());
        }
        if (hasRole(user, "ROLE_STUDENT")) {
            if (user.getStudentGroup() == null) {
                return new ArrayList<>();
            }
            return lectureService.findLecturesByDateAndGroupId(LocalDate.now(), user.getStudentGroup().getId());
        }
        return lectureService.findLecturesByDateAndLecturerId(LocalDate.now(), user.getId());
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName().equals(roleName));
    }

    private void validateLecturePermission(Long lectureId, User user) {
        if (!hasRole(user, "ROLE_ADMIN") && !lectureService.canLecturerManageLecture(lectureId, user.getId())) {
            throw new RuntimeException("თქვენ არ გაქვთ ამ ლექციის მართვის უფლება");
        }
    }
}
