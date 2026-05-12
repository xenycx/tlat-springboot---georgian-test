import sys

filepath = 'src/main/java/com/tlat/controller/AuthController.java'

with open(filepath, 'r') as f:
    content = f.read()

# Add import if missing
if 'java.util.stream.Collectors;' not in content:
    content = content.replace('import java.util.List;', 'import java.util.List;\nimport java.util.stream.Collectors;')

# Replace users method
old_method = """	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/users")
	// მომხმარებელთა სია (ადმინისტრატორი)
	public String users(Model model) {
		List<UserDto> users = userService.findAllUsers();
		model.addAttribute("users", users);
		return "user";
	}"""

new_method = """	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/users")
	// მომხმარებელთა სია (ადმინისტრატორი)
	public String users(
			@RequestParam(required = false) String role,
			@RequestParam(required = false) Long groupId,
			@RequestParam(required = false) String search,
			Model model) {
		
		List<UserDto> users = userService.findAllUsers();

		if (role != null && !role.isEmpty()) {
			users = users.stream().filter(u -> role.equals(u.getRole())).collect(Collectors.toList());
		}
		if (groupId != null) {
			users = users.stream().filter(u -> groupId.equals(u.getGroupId())).collect(Collectors.toList());
		}
		if (search != null && !search.trim().isEmpty()) {
			String q = search.toLowerCase().trim();
			users = users.stream().filter(u -> 
				(u.getFirstName() != null && u.getFirstName().toLowerCase().contains(q)) ||
				(u.getLastName() != null && u.getLastName().toLowerCase().contains(q)) ||
				(u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
			).collect(Collectors.toList());
		}

		model.addAttribute("users", users);
		model.addAttribute("groups", studentGroupService.findAllGroups());
		model.addAttribute("selectedRole", role);
		model.addAttribute("selectedGroupId", groupId);
		model.addAttribute("searchQuery", search);
		
		return "user";
	}"""

if old_method in content:
    content = content.replace(old_method, new_method)
    with open(filepath, 'w') as f:
        f.write(content)
    print("Patch applied.")
else:
    print("Method not found.")

