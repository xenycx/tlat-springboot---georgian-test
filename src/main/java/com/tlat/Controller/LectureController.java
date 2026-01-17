package com.tlat.Controller;

import com.tlat.Dto.LectureDto;
import com.tlat.service.LectureService;
import com.tlat.service.RoomService;
import com.tlat.service.UserService;
import com.tlat.service.PdfExportService;
import com.tlat.Entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;
import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/lectures")
public class LectureController {

    private final LectureService lectureService;
    private final RoomService roomService;
    private final UserService userService;
    private final PdfExportService pdfExportService;

    @Autowired
    public LectureController(LectureService lectureService, 
                            RoomService roomService,
                            UserService userService,
                            PdfExportService pdfExportService) {
        this.lectureService = lectureService;
        this.roomService = roomService;
        this.userService = userService;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping
public String listLectures(Model model, Principal principal) {
    // Get logged in user
    User user = userService.findUserByEmail(principal.getName());
    String fullName = user.getName();
    
    List<LectureDto> lectures = new ArrayList<>(); // Initialize empty list
    
    try {
        // If user has ADMIN role, show all lectures
        if (user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))) {
            lectures = lectureService.findAllLectures();
        } else {
            // For regular users, show only their lectures
            lectures = lectureService.findLecturesByLecturer(fullName);
        }
    } catch (Exception e) {
        // Log the error
        e.printStackTrace();
    }
    
    model.addAttribute("lectures", lectures != null ? lectures : new ArrayList<>());
    return "lecture/list";
}

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("lecture", new LectureDto());
        model.addAttribute("rooms", roomService.findAllRooms());
        model.addAttribute("users", userService.findAllUsers());
        return "lecture/add";
    }
    @PostMapping("/add")
    public String addLecture(@jakarta.validation.Valid @ModelAttribute LectureDto lecture, 
                           org.springframework.validation.BindingResult result) {
        if (result.hasErrors()) {
            return "lecture/add";
        }
        lectureService.saveLecture(lecture);
        return "redirect:/lectures";
    }

    @PostMapping("/import")
    public String importCsv(@RequestParam("file") MultipartFile file) {
        lectureService.importLecturesFromCsv(file);
        return "redirect:/lectures";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        LectureDto lecture = lectureService.findLectureById(id);
        model.addAttribute("lecture", lecture);
        model.addAttribute("rooms", roomService.findAllRooms());
        model.addAttribute("users", userService.findAllUsers());
        return "lecture/edit";
    }

    @PostMapping("/edit/{id}")
    public String editLecture(@jakarta.validation.Valid @ModelAttribute LectureDto lecture, 
                            org.springframework.validation.BindingResult result,
                            @PathVariable Long id) {
        if (result.hasErrors()) {
            return "lecture/edit";
        }
        lectureService.editLecture(lecture, id);
        return "redirect:/lectures";
    }

    @GetMapping("/delete/{id}")
    public String deleteLecture(@PathVariable Long id) {
        lectureService.deleteLectureById(id);
        return "redirect:/lectures";
    }

    // ლექციის დაწყების POST Endpoint-ი
    // პარამეტრები:
    // - id: ლექციის იდენტიიკატორი
    // - request: მიმდინარე HTTP მოთხოვნა (IP, user-agent და ა.შ. გადასაცემად service-ს)
    // - redirectAttributes: შეტყობინებების გადამისამართებისთვის (flash attributes)
    // - referer: მიმღები გვერდი, საიდანაც მოვიდა მოთხოვნა (რედირექტისთვის)
    @PostMapping("/start/{id}")
    public String startLecture(@PathVariable Long id, 
                             HttpServletRequest request, 
                             RedirectAttributes redirectAttributes,
                             @RequestHeader(value = "Referer", required = false) String referer) {
        try {
            // სცადე ლექციის დაწყება სერვისში
            lectureService.startLecture(id, request);
            // წარმატების შეტყობინება
            redirectAttributes.addFlashAttribute("successMessage", "ლექცია წარმატებით დაიწყო");
        } catch (Exception e) {
            // შეცდომის შეტყობინება - გამოიტანე შეცდომის ტექსტი
            redirectAttributes.addFlashAttribute("errorMessage", egetMessage());
        }
        // დაბრუნება: თუ Referer არსებობს, დააბრუნე იმ გვერდზე; წინააღმდეგ შემთხვევაში /lectures
        return referer != null ? "redirect:" + referer : "redirect:/lectures";
    }

    @PostMapping("/stop/{id}")
    public String stopLecture(@PathVariable Long id, 
                            HttpServletRequest request, 
                            RedirectAttributes redirectAttributes,
                            @RequestHeader(value = "Referer", required = false) String referer) {
        try {
            lectureService.stopLecture(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "ლექცია წარმატებით დასრულდა");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return referer != null ? "redirect:" + referer : "redirect:/lectures";
    }

    // ============= Export Endpoints =============
    
    /**
     * Export a single lecture as PDF
     */
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

    /**
     * Export all lectures as PDF
     */
    @GetMapping("/export/pdf/all")
    public ResponseEntity<byte[]> exportAllLecturesPdf(Principal principal) {
        try {
            User user = userService.findUserByEmail(principal.getName());
            List<LectureDto> lectures;
            
            // Check if user is admin
            if (user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))) {
                lectures = lectureService.findAllLectures();
            } else {
                lectures = lectureService.findLecturesByLecturer(user.getName());
            }
            
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

    /**
     * Export all lectures as Excel
     */
    @GetMapping("/export/excel/all")
    public ResponseEntity<byte[]> exportAllLecturesExcel(Principal principal) {
        try {
            User user = userService.findUserByEmail(principal.getName());
            List<LectureDto> lectures;
            
            // Check if user is admin
            if (user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))) {
                lectures = lectureService.findAllLectures();
            } else {
                lectures = lectureService.findLecturesByLecturer(user.getName());
            }
            
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

    /**
     * Export filtered lectures by date range
     */
    @GetMapping("/export/pdf/filtered")
    public ResponseEntity<byte[]> exportFilteredLecturesPdf(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String status,
            Principal principal) {
        try {
            User user = userService.findUserByEmail(principal.getName());
            List<LectureDto> allLectures;
            
            // Check if user is admin
            if (user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))) {
                allLectures = lectureService.findAllLectures();
            } else {
                allLectures = lectureService.findLecturesByLecturer(user.getName());
            }
            
            // Apply filters
            List<LectureDto> filteredLectures = allLectures.stream()
                .filter(lecture -> {
                    boolean matches = true;
                    
                    if (startDate != null && !startDate.isEmpty()) {
                        LocalDate start = LocalDate.parse(startDate);
                        matches = matches && !lecture.getDate().isBefore(start);
                    }
                    
                    if (endDate != null && !endDate.isEmpty()) {
                        LocalDate end = LocalDate.parse(endDate);
                        matches = matches && !lecture.getDate().isAfter(end);
                    }
                    
                    if (status != null && !status.isEmpty() && !status.equals("ALL")) {
                        matches = matches && lecture.getStatus().toString().equals(status);
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

    /**
     * Export today's lectures as PDF
     */
    @GetMapping("/export/pdf/today")
    public ResponseEntity<byte[]> exportTodayLecturesPdf(Principal principal) {
        try {
            User user = userService.findUserByEmail(principal.getName());
            List<LectureDto> todaysLectures;
            
            // Check if user is admin
            if (user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))) {
                todaysLectures = lectureService.findLecturesByDate(LocalDate.now());
            } else {
                todaysLectures = lectureService.findLecturesByDateAndLecturer(LocalDate.now(), user.getName());
            }
            
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

    /**
     * Export today's lectures as Excel
     */
    @GetMapping("/export/excel/today")
    public ResponseEntity<byte[]> exportTodayLecturesExcel(Principal principal) {
        try {
            User user = userService.findUserByEmail(principal.getName());
            List<LectureDto> todaysLectures;
            
            // Check if user is admin
            if (user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))) {
                todaysLectures = lectureService.findLecturesByDate(LocalDate.now());
            } else {
                todaysLectures = lectureService.findLecturesByDateAndLecturer(LocalDate.now(), user.getName());
            }
            
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
}