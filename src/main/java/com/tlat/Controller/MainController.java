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
import java.util.ArrayList;
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

        List<LectureDto> todaysLectures;
        List<LectureDto> upcomingLectures = new ArrayList<>();
        boolean isStudent = hasRole(user, "ROLE_STUDENT");

        if (hasRole(user, "ROLE_ADMIN")) {
            todaysLectures = lectureService.findLecturesByDate(LocalDate.now());
        } else if (isStudent) {
            if (user.getStudentGroup() == null) {
                todaysLectures = new ArrayList<>();
            } else {
                Long groupId = user.getStudentGroup().getId();
                todaysLectures = lectureService.findLecturesByDateAndGroupId(LocalDate.now(), groupId);
                upcomingLectures = lectureService.findUpcomingLecturesByGroupId(groupId, LocalDate.now());
            }
        } else {
            todaysLectures = lectureService.findLecturesByDateAndLecturerId(LocalDate.now(), user.getId());
        }
        model.addAttribute("todaysLectures", todaysLectures);
        model.addAttribute("upcomingLectures", upcomingLectures);
        model.addAttribute("isStudent", isStudent);

        return "main";
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName().equals(roleName));
    }
}
