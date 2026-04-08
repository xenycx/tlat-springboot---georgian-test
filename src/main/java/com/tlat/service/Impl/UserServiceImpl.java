package com.tlat.service.Impl;

import com.tlat.Dto.UserDto;
import com.tlat.Entity.Role;
import com.tlat.Entity.StudentGroup;
import com.tlat.Entity.User;
import com.tlat.Repository.RoleRepository;
import com.tlat.Repository.StudentGroupRepository;
import com.tlat.Repository.UserRepository;
import com.tlat.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StudentGroupRepository studentGroupRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeRolesAndMigrateLegacyRole() {
        dropLegacyIsPaidColumnIfExists();
        Role lecturerRole = ensureRoleExists("ROLE_LECTURER");
        ensureRoleExists("ROLE_STUDENT");
        ensureRoleExists("ROLE_ADMIN");

        Role legacyUserRole = roleRepository.findByName("ROLE_USER");
        if (legacyUserRole == null) {
            return;
        }

        List<User> users = userRepository.findAll();
        for (User user : users) {
            boolean hasLegacyRole = user.getRoles().stream()
                    .anyMatch(role -> "ROLE_USER".equals(role.getName()));
            if (!hasLegacyRole) {
                continue;
            }

            user.getRoles().removeIf(role -> "ROLE_USER".equals(role.getName()));
            boolean alreadyLecturer = user.getRoles().stream()
                    .anyMatch(role -> "ROLE_LECTURER".equals(role.getName()));
            if (!alreadyLecturer) {
                user.getRoles().add(lecturerRole);
            }
        }
        userRepository.saveAll(users);
        roleRepository.delete(legacyUserRole);
    }

    private void dropLegacyIsPaidColumnIfExists() {
        try {
            String databaseProduct = jdbcTemplate.execute((ConnectionCallback<String>) connection ->
                    connection.getMetaData().getDatabaseProductName());
            if (databaseProduct == null || !databaseProduct.toLowerCase().contains("mysql")) {
                log.info("Skipping users.is_paid cleanup for database type: {}", databaseProduct);
                return;
            }

            Integer columnCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                    Integer.class,
                    "users",
                    "is_paid"
            );
            if (columnCount != null && columnCount > 0) {
                log.info("Dropping legacy column users.is_paid");
                jdbcTemplate.execute("ALTER TABLE users DROP COLUMN is_paid");
                log.info("Dropped legacy column users.is_paid");
            } else {
                log.info("Legacy column users.is_paid not found; skipping");
            }
        } catch (DataAccessException ex) {
            log.error("Failed while cleaning up legacy column users.is_paid", ex);
            throw ex;
        }
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
        StudentGroup group = studentGroupRepository.findById(Objects.requireNonNull(userDto.getGroupId(), "groupId is required"))
                .orElseThrow(() -> new EntityNotFoundException("Student group not found"));
        user.setStudentGroup(group);
    }
}
