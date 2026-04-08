package com.tlat.Controller;

import com.tlat.Dto.ResourceFormDto;
import com.tlat.Entity.LearningResource;
import com.tlat.Entity.User;
import com.tlat.service.LearningResourceService;
import com.tlat.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Objects;
import java.util.List;

@Controller
public class LearningResourceController {

    private final LearningResourceService learningResourceService;
    private final UserService userService;

    @Autowired
    public LearningResourceController(LearningResourceService learningResourceService,
                                      UserService userService) {
        this.learningResourceService = learningResourceService;
        this.userService = userService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @GetMapping("/resources")
    public String resourcesPage(Model model, Principal principal) {
        User user = getCurrentUser(principal);
        model.addAttribute("user", user);
        model.addAttribute("resources", learningResourceService.getManageResources(user));
        model.addAttribute("isAdmin", hasRole(user, "ROLE_ADMIN"));
        return "resource/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @GetMapping("/resources/add")
    public String addResourceForm(Model model, Principal principal) {
        User user = getCurrentUser(principal);
        model.addAttribute("user", user);
        model.addAttribute("resourceForm", new ResourceFormDto());
        model.addAttribute("lectures", learningResourceService.getSelectableLectures(user));
        return "resource/add";
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/resources/add")
    public String addResource(@Valid @ModelAttribute("resourceForm") ResourceFormDto form,
                              BindingResult result,
                              @RequestParam("file") MultipartFile file,
                              Principal principal,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);

        if (result.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("lectures", learningResourceService.getSelectableLectures(user));
            return "resource/add";
        }

        try {
            learningResourceService.createResource(form, file, user);
            redirectAttributes.addFlashAttribute("successMessage", "ფაილი წარმატებით აიტვირთა");
            return "redirect:/resources";
        } catch (Exception e) {
            model.addAttribute("user", user);
            model.addAttribute("lectures", learningResourceService.getSelectableLectures(user));
            model.addAttribute("errorMessage", e.getMessage());
            return "resource/add";
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @GetMapping("/resources/edit/{id}")
    public String editResourceForm(@PathVariable Long id, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);
        try {
            LearningResource resource = learningResourceService.getEditableResource(id, user);
            ResourceFormDto form = new ResourceFormDto();
            form.setLectureId(resource.getLecture().getId());
            form.setTitle(resource.getTitle());
            form.setDescription(resource.getDescription());
            form.setCategory(resource.getCategory());
            form.setPublished(resource.getPublishStatus().name().equals("PUBLISHED"));
            form.setVisibleFrom(resource.getVisibleFrom());
            form.setVisibleUntil(resource.getVisibleUntil());

            model.addAttribute("resourceId", id);
            model.addAttribute("resource", resource);
            model.addAttribute("resourceForm", form);
            model.addAttribute("lectures", learningResourceService.getSelectableLectures(user));
            model.addAttribute("user", user);
            return "resource/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/resources";
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/resources/edit/{id}")
    public String editResource(@PathVariable Long id,
                               @Valid @ModelAttribute("resourceForm") ResourceFormDto form,
                               BindingResult result,
                               @RequestParam(value = "file", required = false) MultipartFile file,
                               Principal principal,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);

        if (result.hasErrors()) {
            model.addAttribute("resourceId", id);
            model.addAttribute("user", user);
            model.addAttribute("lectures", learningResourceService.getSelectableLectures(user));
            return "resource/edit";
        }

        try {
            learningResourceService.updateResource(id, form, file, user);
            redirectAttributes.addFlashAttribute("successMessage", "ფაილის ინფორმაცია განახლდა");
            return "redirect:/resources";
        } catch (Exception e) {
            model.addAttribute("resourceId", id);
            model.addAttribute("user", user);
            model.addAttribute("lectures", learningResourceService.getSelectableLectures(user));
            model.addAttribute("errorMessage", e.getMessage());
            return "resource/edit";
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/resources/publish/{id}")
    public String publish(@PathVariable Long id,
                          Principal principal,
                          RedirectAttributes redirectAttributes,
                          @RequestHeader(value = "Referer", required = false) String referer) {
        User user = getCurrentUser(principal);
        try {
            learningResourceService.setPublished(id, true, user);
            redirectAttributes.addFlashAttribute("successMessage", "ფაილი გამოქვეყნდა");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return referer != null ? "redirect:" + referer : "redirect:/resources";
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/resources/unpublish/{id}")
    public String unpublish(@PathVariable Long id,
                            Principal principal,
                            RedirectAttributes redirectAttributes,
                            @RequestHeader(value = "Referer", required = false) String referer) {
        User user = getCurrentUser(principal);
        try {
            learningResourceService.setPublished(id, false, user);
            redirectAttributes.addFlashAttribute("successMessage", "ფაილი გადაყვანილია Draft რეჟიმში");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return referer != null ? "redirect:" + referer : "redirect:/resources";
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/resources/delete/{id}")
    public String delete(@PathVariable Long id,
                         Principal principal,
                         RedirectAttributes redirectAttributes,
                         @RequestHeader(value = "Referer", required = false) String referer) {
        User user = getCurrentUser(principal);
        try {
            learningResourceService.deleteResource(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "ფაილი წაიშალა");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return referer != null ? "redirect:" + referer : "redirect:/resources";
    }

    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @PostMapping("/resources/delete-selected")
    public String deleteSelected(@RequestParam("ids") List<Long> ids,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        User user = getCurrentUser(principal);
        int deleted = 0;
        int failed = 0;
        for (Long id : ids) {
            try {
                learningResourceService.deleteResource(id, user);
                deleted++;
            } catch (Exception e) {
                failed++;
            }
        }
        if (failed > 0) {
            redirectAttributes.addFlashAttribute("errorMessage", deleted + " ფაილი წაიშალა, " + failed + " ვერ წაიშალა");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", deleted + " ფაილი წარმატებით წაიშალა");
        }
        return "redirect:/resources";
    }

    @GetMapping("/resources/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id, Principal principal) {
        User user = getCurrentUser(principal);
        LearningResourceService.ResourceDownloadPayload payload = learningResourceService.getDownloadPayload(id, user);
        Resource resource = new FileSystemResource(Objects.requireNonNull(payload.filePath()));

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(Objects.requireNonNull(payload.contentType())))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(payload.originalFilename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(resource);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/materials")
    public String studentMaterials(Model model, Principal principal) {
        User user = getCurrentUser(principal);
        List<LearningResource> resources = learningResourceService.getVisibleResourcesForStudent(user);
        model.addAttribute("user", user);
        model.addAttribute("resources", resources);
        return "resource/student";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/resources/audit")
    public String auditLog(Model model, Principal principal) {
        User user = getCurrentUser(principal);
        model.addAttribute("user", user);
        model.addAttribute("logs", learningResourceService.getAuditLogs(user));
        return "resource/audit";
    }

    private User getCurrentUser(Principal principal) {
        return userService.findUserByEmail(principal.getName());
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName().equals(roleName));
    }
}
