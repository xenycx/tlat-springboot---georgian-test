package com.tlat.Controller;

import com.tlat.Dto.LectureDto;
import com.tlat.Entity.User;
import com.tlat.service.LectureService;
import com.tlat.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * მთავარი გვერდის კონტროლერი — მზადდება model და გადაეცემა "main" თემფლეიტს.
 */
@Controller
@RequestMapping("/main")
public class MainController {

    private final UserService userService;
    private final LectureService lectureService;

    @Autowired
    public MainController(UserService userService, LectureService lectureService) {
        this.userService = userService;
        this.lectureService = lectureService;
    }

    /**
     * მთავარი გვერდი: ატვირთავს ავტორიზებულ მომხმარებელს და დღევანდელ ლექციებს model-ში.
     */
    @GetMapping
    public String mainPage(Model model, Principal principal) {
        User user = userService.findUserByEmail(principal.getName());
        model.addAttribute("user", user);

        // მოიპოვეთ დღევანდელი ლექციები ავტორიზებული მომხმარებლისთვის
        List<LectureDto> todaysLectures;
        if (user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))) {
            // ადმინი ხედავს ყველა ლექციას
            todaysLectures = lectureService.findLecturesByDate(LocalDate.now());
        } else {
            // ჩვეულებრივი მომხმარებლები ხედავენ მხოლოდ საკუთარ ლექციებს
            todaysLectures = lectureService.findLecturesByDateAndLecturer(LocalDate.now(), user.getName());
        }
        model.addAttribute("todaysLectures", todaysLectures);
        
        return "main";
    }
}