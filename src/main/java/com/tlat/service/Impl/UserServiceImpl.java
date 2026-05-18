package com.tlat.service.Impl;

import com.tlat.dto.UserDto;
import com.tlat.entity.Role;
import com.tlat.entity.StudentGroup;
import com.tlat.entity.User;
import com.tlat.repository.RoleRepository;
import com.tlat.repository.StudentGroupRepository;
import com.tlat.repository.UserRepository;
import com.tlat.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StudentGroupRepository studentGroupRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeRolesAndMigrateLegacyRole() {
        log.info("Starting roles initialization and legacy role migration...");
        Role lecturerRole = ensureRoleExists("ROLE_LECTURER");
        ensureRoleExists("ROLE_STUDENT");
        ensureRoleExists("ROLE_ADMIN");

        Role legacyUserRole = roleRepository.findByName("ROLE_USER");
        if (legacyUserRole == null) {
            log.info("No ROLE_USER found, skipping migration.");
            return;
        }

        // Only fetch users that actually have the legacy role
        List<User> usersWithLegacyRole = userRepository.findAllByRoles_Name("ROLE_USER");
        if (usersWithLegacyRole.isEmpty()) {
            log.info("No users found with ROLE_USER. Cleaning up legacy role...");
            roleRepository.delete(legacyUserRole);
            return;
        }

        log.info("Found {} users with legacy ROLE_USER to migrate.", usersWithLegacyRole.size());

        // Process in smaller chunks to avoid memory pressure and long transactions
        final int batchSize = 100;
        for (int i = 0; i < usersWithLegacyRole.size(); i += batchSize) {
            int end = Math.min(i + batchSize, usersWithLegacyRole.size());
            List<User> batch = new ArrayList<>(usersWithLegacyRole.subList(i, end));

            for (User user : batch) {
                user.getRoles().removeIf(role -> "ROLE_USER".equals(role.getName()));
                boolean alreadyLecturer = user.getRoles().stream()
                        .anyMatch(role -> "ROLE_LECTURER".equals(role.getName()));
                if (!alreadyLecturer) {
                    user.getRoles().add(lecturerRole);
                }
            }
            userRepository.saveAll(batch);
            log.info("Migrated batch of {} users ({} to {}).", batch.size(), i, end);
        }

        log.info("Deleting legacy ROLE_USER...");
        roleRepository.delete(legacyUserRole);
        log.info("Roles initialization and legacy role migration completed.");
    }

    @Transactional
    public void saveUser(UserDto userDto) {
        User user = new User();
        user.setName(userDto.getFirstName() + " " + userDto.getLastName());
        user.setEmail(userDto.getEmail());
        user.setUsername(userDto.getUsername() != null ? userDto.getUsername() : userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setAge(userDto.getAge());
        user.setPhone(userDto.getPhone());
        user.setGender(userDto.getGender());
        user.setAddress(userDto.getAddress());

        String roleName = (userDto.getRole() != null && !userDto.getRole().isBlank())
                ? userDto.getRole()
                : "ROLE_LECTURER";
        Role role = ensureRoleExists(roleName);
        user.setRoles(new ArrayList<>(List.of(role)));

        assignStudentGroupIfNeeded(user, userDto, roleName);
        userRepository.save(user);
    }

    @Override
    public void editUser(UserDto updatedUserDto, Long userId) {
        Long nonNullUserId = Objects.requireNonNull(userId, "userId is required");
        User existingUser = userRepository.findById(nonNullUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        existingUser.setName(updatedUserDto.getFirstName() + " " + updatedUserDto.getLastName());
        existingUser.setAge(updatedUserDto.getAge());
        existingUser.setPhone(updatedUserDto.getPhone());
        existingUser.setGender(updatedUserDto.getGender());
        existingUser.setAddress(updatedUserDto.getAddress());

        if (updatedUserDto.getEmail() != null && !updatedUserDto.getEmail().isEmpty()) {
            existingUser.setEmail(updatedUserDto.getEmail());
        }

        if (updatedUserDto.getUsername() != null && !updatedUserDto.getUsername().isEmpty()) {
            existingUser.setUsername(updatedUserDto.getUsername());
        } else if (updatedUserDto.getEmail() != null && !updatedUserDto.getEmail().isEmpty()) {
            existingUser.setUsername(updatedUserDto.getEmail());
        }

        if (updatedUserDto.getPassword() != null && !updatedUserDto.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(updatedUserDto.getPassword()));
        }

        String effectiveRole = existingUser.getRoles().isEmpty()
                ? "ROLE_LECTURER"
                : existingUser.getRoles().get(0).getName();
        if (updatedUserDto.getRole() != null && !updatedUserDto.getRole().isEmpty()) {
            Role newRole = ensureRoleExists(updatedUserDto.getRole());
            existingUser.getRoles().clear();
            existingUser.getRoles().add(newRole);
            effectiveRole = newRole.getName();
        }

        assignStudentGroupIfNeeded(existingUser, updatedUserDto, effectiveRole);
        userRepository.save(existingUser);
    }

    public void deleteUserById(Long userId) {
        Optional<User> userOptional = userRepository.findById(Objects.requireNonNull(userId, "userId is required"));
        userOptional.ifPresent(user -> {
            user.getRoles().clear();
            userRepository.delete(user);
        });
    }

    public boolean doesUserExist(Long userId) {
        Optional<User> userOptional = userRepository.findById(Objects.requireNonNull(userId, "userId is required"));
        return userOptional.isPresent();
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public UserDto findUserById(Long userId) {
        Optional<User> userOptional = userRepository.findById(Objects.requireNonNull(userId, "userId is required"));
        if (userOptional.isPresent()) {
            return mapToUserDto(userOptional.get());
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> findAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> findUsersByRole(String roleName) {
        return userRepository.findAllByRoles_Name(roleName).stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());
    }

    private UserDto mapToUserDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        String[] str = user.getName().split(" ");
        userDto.setFirstName(str[0]);
        userDto.setLastName(str.length > 1 ? str[1] : "");
        userDto.setEmail(user.getEmail());
        userDto.setUsername(user.getUsername());
        userDto.setAge(user.getAge());
        userDto.setPhone(user.getPhone());
        userDto.setGender(user.getGender());
        userDto.setAddress(user.getAddress());
        userDto.setRole(user.getRoles().isEmpty() ? null : user.getRoles().get(0).getName());
        userDto.setAvatarPath(user.getAvatarPath());
        if (user.getStudentGroup() != null) {
            userDto.setGroupId(user.getStudentGroup().getId());
            userDto.setGroupCode(user.getStudentGroup().getCode());
        }
        return userDto;
    }

    @Override
    public void updateUserAvatar(Long userId, String avatarPath) {
        User user = userRepository.findById(Objects.requireNonNull(userId, "userId is required"))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setAvatarPath(avatarPath);
        userRepository.save(user);
    }

    private Role ensureRoleExists(String roleName) {
        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            role = roleRepository.save(new Role(null, roleName, null));
        }
        return role;
    }

    private void assignStudentGroupIfNeeded(User user, UserDto userDto, String roleName) {
        if (!"ROLE_STUDENT".equals(roleName)) {
            user.setStudentGroup(null);
            return;
        }
        if (userDto.getGroupId() == null) {
            throw new IllegalArgumentException("Student group is required");
        }
        StudentGroup group = studentGroupRepository
                .findById(Objects.requireNonNull(userDto.getGroupId(), "groupId is required"))
                .orElseThrow(() -> new EntityNotFoundException("Student group not found"));
        user.setStudentGroup(group);
    }
}
