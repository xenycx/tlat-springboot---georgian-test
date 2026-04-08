package com.tlat.Controller;

import com.tlat.Dto.UserDto;
import com.tlat.Entity.User;
import com.tlat.service.AvatarService;
import com.tlat.service.StudentGroupService;
import com.tlat.service.UserService;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * ავტორიზაციისა და მომხმარებელთა მართვის კონტროლერი.
 * შეიცავს რეგისტრაციას, ავტორიზაციას, მომხმარებლის CRUD ოპერაციებს და ავატარის მართვას.
 */
@Controller
public class AuthController {

	@Autowired
	private UserService userService;

	@Autowired
	private AvatarService avatarService;

	@Autowired
	private StudentGroupService studentGroupService;

	// კონსტრუქტორი ინექციით
	public AuthController(UserService userService, AvatarService avatarService, StudentGroupService studentGroupService) {
		this.userService = userService;
		this.avatarService = avatarService;
		this.studentGroupService = studentGroupService;
	}

	@ModelAttribute("currentUser")
	public User currentUser(Principal principal) {
		if (principal == null) {
			return null;
		}
		return userService.findUserByEmail(principal.getName());
	}

	@GetMapping("/")
	// მთავარი გვერდი -> გადამისამართება შესვლაზე
	public String home() {
		return "redirect:/login";
	}

	@GetMapping("/login")
	// შესვლის ფর্মა: თუ უკვე რეგისტრირებული და ავთენთიკაცია მოქმედებს, გადამისამართება მთავარ გვერდზე
	public String loginForm(Authentication authentication) {
		// გამოიყენება ინექცირებული Authentication, რომ თავიდან ავიცილოთ გამოსვლის შემდეგ სესიის დროითი პრობლემები
		if (authentication != null && authentication.isAuthenticated()
				&& !"anonymousUser".equals(authentication.getPrincipal())) {
			return "redirect:/main";
		}
		return "login";
	}

	@GetMapping("/register")
	// რეგისტრაციის ფორმა: თუ უკვე ავტორიზებული მომხმარებელი არ გაუშვებს რეგისტრაციის ფორტმას
	public String showRegistrationForm(Model model) {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			UserDto user = new UserDto();
			model.addAttribute("user", user);
			if (authentication != null
					&& authentication.isAuthenticated()
					&& !"anonymousUser".equals(authentication.getPrincipal())) {
				return "redirect:/users";
			} else {

				return "register";
			}
		}

	@PostMapping("/register/save")
	// მომხმარებლის რეგისტრაციის დამუშავება და ავატარის ატვირთვა (თუ მოწოდებულია)
	public String registration(
			@Valid @ModelAttribute("user") UserDto userDto,
			BindingResult result,
			@RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
			Model model) {

		User existingUser = userService.findUserByEmail(userDto.getEmail());

		if (existingUser != null && existingUser.getEmail() != null && !existingUser.getEmail().isEmpty()) {
			result.rejectValue("email", "",
					"უკვე არის ამ მეილის დარეგისტრირებული მომხმარებელი");
		}

		if (!userDto.getPassword().isEmpty()) {
			if (userDto.getPassword().length() < 7) {
				result.rejectValue("password", "field.min.length", "პაროლი უნდა შეიცავდეს მინიმუმ 7 სიმბოლოს");
			}
		}else{
			result.rejectValue("password", "field.min.length", "პაროლი არ უნდა იყოს ცარიელი.");
		}

		if (result.hasErrors()) {
			model.addAttribute("user", userDto);
			return "register";
		}

		userService.saveUser(userDto);

		// ავატარის ატვირთვის ოპერაცია მომხმარებლის შენახვის შემდეგ
		if (avatarFile != null && !avatarFile.isEmpty()) {
			try {
				User savedUser = userService.findUserByEmail(userDto.getEmail());
				String avatarPath = avatarService.saveAvatar(avatarFile, savedUser.getId());
				userService.updateUserAvatar(savedUser.getId(), avatarPath);
			} catch (IOException e) {
				// შეცდომის ლოგირება, მაგრამ გაგრძელება — ავატარის ატვირთვის შეცდომა რეგისტრაციას არ უნდა ჩაეშალოს
				System.err.println("Failed to save avatar: " + e.getMessage());
			}
		}

		return "redirect:/register?success=true";
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/add/save")
	// ადმინისტრატორის მიერ ახალი მომხმარებლის დამატება და ავატარის ატვირთვა
	public String addUser(
			@Valid @ModelAttribute("user") UserDto userDto,
			BindingResult result,
			@RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
			Model model) {
		User existingUser = userService.findUserByEmail(userDto.getEmail());

		if (existingUser != null && existingUser.getEmail() != null && !existingUser.getEmail().isEmpty()) {
			result.rejectValue("email", "",
					"There is already an account registered with the same email");
		}

		if (!userDto.getPassword().isEmpty()) {
			if (userDto.getPassword().length() < 7) {
				result.rejectValue("password", "field.min.length", "Password should have at least 7 characters");
			}
		}else{
			result.rejectValue("password", "field.min.length", "Password should not be empty.");
		}

		if (isStudentRole(userDto) && userDto.getGroupId() == null) {
			result.rejectValue("groupId", "", "სტუდენტისთვის ჯგუფის არჩევა სავალდებულოა");
		}

		if (result.hasErrors()) {
			model.addAttribute("user", userDto);
			model.addAttribute("groups", studentGroupService.findAllGroups());
			return "add";
		}

		try {
			userService.saveUser(userDto);
		} catch (IllegalArgumentException e) {
			result.rejectValue("groupId", "", "სტუდენტისთვის ჯგუფის არჩევა სავალდებულოა");
			model.addAttribute("user", userDto);
			model.addAttribute("groups", studentGroupService.findAllGroups());
			return "add";
		}

		// ავატარის ატვირთვა დამატებისთანავე
		if (avatarFile != null && !avatarFile.isEmpty()) {
			try {
				User savedUser = userService.findUserByEmail(userDto.getEmail());
				String avatarPath = avatarService.saveAvatar(avatarFile, savedUser.getId());
				userService.updateUserAvatar(savedUser.getId(), avatarPath);
			} catch (IOException e) {
				System.err.println("Failed to save avatar: " + e.getMessage());
			}
		}

		return "redirect:/users?success=true";
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/add")
	// ადმინისტრატორის ფর্মა ახალი მომხმარებლის შექმნისათვის
	public String showUserAddForm(Model model) {
		UserDto user = new UserDto();
		model.addAttribute("user", user);
		model.addAttribute("groups", studentGroupService.findAllGroups());
		return "add";
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/users")
	// მომხმარებელთა სია (ადმინისტრატორი)
	public String users(Model model) {
		List<UserDto> users = userService.findAllUsers();
		model.addAttribute("users", users);
		return "user";
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/edit/{id}")
	// მომხმარებლის რედაქტირების ფორმა
	public String editUser(
			@PathVariable Long id,
			Model model) {
		UserDto user = userService.findUserById(id);
		model.addAttribute("user", user);
		model.addAttribute("groups", studentGroupService.findAllGroups());
		return "edit";
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/edit/{id}")
	// მოსატანად მომხმარებლის ინფორმაციის განახლება და ავატარის შეცვლა თუ საჭირო
	public String updateUserById(
			@Valid @ModelAttribute("user") UserDto updatedUserDto,
			BindingResult result,
			@PathVariable Long id,
			@RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
			Model model) {

		if (!updatedUserDto.getPassword().isEmpty()) {
			if (updatedUserDto.getPassword().length() < 7) {
				result.rejectValue("password", "field.min.length", "Password should have at least 7 characters");
			}
		}

		if (isStudentRole(updatedUserDto) && updatedUserDto.getGroupId() == null) {
			result.rejectValue("groupId", "", "სტუდენტისთვის ჯგუფის არჩევა სავალდებულოა");
		}

		if (result.hasErrors()) {
			model.addAttribute("user", updatedUserDto);
			model.addAttribute("groups", studentGroupService.findAllGroups());
			return "edit";
		}

		// ავატარის ატვირთვის დამუშავება
		if (avatarFile != null && !avatarFile.isEmpty()) {
			try {
				// ძველი ავატარის წაშლა, თუ არსებობს
				UserDto existingUser = userService.findUserById(id);
				if (existingUser != null && existingUser.getAvatarPath() != null) {
					avatarService.deleteAvatar(existingUser.getAvatarPath());
				}

				String avatarPath = avatarService.saveAvatar(avatarFile, id);
				userService.updateUserAvatar(id, avatarPath);
			} catch (IOException e) {
				System.err.println("Failed to save avatar: " + e.getMessage());
			}
		}

		try {
			userService.editUser(updatedUserDto, id);
		} catch (IllegalArgumentException e) {
			result.rejectValue("groupId", "", "სტუდენტისთვის ჯგუფის არჩევა სავალდებულოა");
			model.addAttribute("user", updatedUserDto);
			model.addAttribute("groups", studentGroupService.findAllGroups());
			return "edit";
		}
		return "redirect:/users";
	}

	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/delete/{id}")
	// მომხმარებლის წაშლა (ადმინისტრატორის ფუნქცია) - თვით-წაშლა არ არის ნებადართული
	public String deleteUser(
			@RequestParam(name = "error", required = false) String error,
			@RequestParam(name = "success", required = false) String success,
			RedirectAttributes redirectAttributes,
			@PathVariable Long id,
			Principal principal,
			Model model) {

		String loggedInUsername = principal.getName();

		User loggedInUser = userService.findUserByEmail(loggedInUsername);

		if (loggedInUser != null && loggedInUser.getId().equals(id)) {
			// თუ ცდილობს თვითონ წაშალოს თავი - ბლოკირება და შეტყობინება
			if (error != null) {
				redirectAttributes.addFlashAttribute("error", "You cannot delete yourself.");
			}
		} else {
			if (userService.doesUserExist(id)) {
				userService.deleteUserById(id);
				if (success != null) {
					redirectAttributes.addFlashAttribute("success", "User has been deleted successfully");
				}
			} else {
				if (error != null) {
					redirectAttributes.addFlashAttribute("error", "User does not exist");
				}
			}
		}
		return "redirect:/users";
	}

	private boolean isStudentRole(UserDto userDto) {
		return "ROLE_STUDENT".equals(userDto.getRole());
	}

}
