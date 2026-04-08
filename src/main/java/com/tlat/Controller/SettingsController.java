package com.tlat.Controller;

import com.tlat.Entity.AppSetting;
import com.tlat.Entity.User;
import com.tlat.service.SettingsService;
import com.tlat.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin/settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final UserService userService;

    public SettingsController(SettingsService settingsService, UserService userService) {
        this.settingsService = settingsService;
        this.userService = userService;
    }

    @GetMapping
    public String settingsPage(Model model, Principal principal) {
        User user = userService.findUserByEmail(principal.getName());
        model.addAttribute("user", user);
        List<AppSetting> settings = settingsService.findAll();
        model.addAttribute("settings", settings);
        return "admin/settings";
    }

    @PostMapping
    public String saveSettings(@RequestParam java.util.Map<String, String> params,
                               RedirectAttributes redirectAttributes) {
        try {
            params.forEach((key, value) -> {
                if (!key.startsWith("_")) { // skip CSRF and other hidden fields
                    settingsService.update(key, value);
                }
            });
            redirectAttributes.addFlashAttribute("successMessage", "პარამეტრები წარმატებით შენახულია");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "შეცდომა: " + e.getMessage());
        }
        return "redirect:/admin/settings";
    }
}
