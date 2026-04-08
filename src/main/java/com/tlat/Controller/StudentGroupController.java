package com.tlat.Controller;

import com.tlat.Dto.StudentGroupDto;
import com.tlat.Entity.User;
import com.tlat.service.StudentGroupService;
import com.tlat.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Objects;
import java.util.Optional;
import java.security.Principal;

@Controller
@RequestMapping("/groups")
@PreAuthorize("hasRole('ADMIN')")
public class StudentGroupController {

    private final StudentGroupService studentGroupService;
    private final UserService userService;

    public StudentGroupController(StudentGroupService studentGroupService, UserService userService) {
        this.studentGroupService = studentGroupService;
        this.userService = userService;
    }

    @ModelAttribute("currentUser")
    public User currentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userService.findUserByEmail(principal.getName());
    }

    @GetMapping
    public String listGroups(Model model) {
        model.addAttribute("groups", studentGroupService.findAllGroups());
        return "group/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("group", new StudentGroupDto());
        return "group/add";
    }

    @PostMapping("/add")
    public String addGroup(@Valid @ModelAttribute("group") StudentGroupDto groupDto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("group", groupDto);
            return "group/add";
        }
        try {
            studentGroupService.saveGroup(groupDto);
        } catch (IllegalArgumentException e) {
            String errorMessage = Optional.ofNullable(e.getMessage()).orElse("ჯგუფის დამატებისას დაფიქსირდა შეცდომა");
            result.rejectValue("code", "error.group", Objects.requireNonNull(errorMessage, "Error message must not be null"));
            model.addAttribute("group", groupDto);
            return "group/add";
        }
        return "redirect:/groups";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("group", studentGroupService.findGroupById(id));
        return "group/edit";
    }

    @PostMapping("/edit/{id}")
    public String editGroup(@Valid @ModelAttribute("group") StudentGroupDto groupDto,
                            BindingResult result,
                            @PathVariable Long id,
                            Model model) {
        if (result.hasErrors()) {
            model.addAttribute("group", groupDto);
            return "group/edit";
        }
        try {
            studentGroupService.editGroup(groupDto, id);
        } catch (IllegalArgumentException e) {
            String errorMessage = Optional.ofNullable(e.getMessage()).orElse("ჯგუფის რედაქტირებისას დაფიქსირდა შეცდომა");
            result.rejectValue("code", "error.group", Objects.requireNonNull(errorMessage, "Error message must not be null"));
            model.addAttribute("group", groupDto);
            return "group/edit";
        }
        return "redirect:/groups";
    }

    @GetMapping("/delete/{id}")
    public String deleteGroup(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            studentGroupService.deleteGroupById(id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/groups";
    }
}
