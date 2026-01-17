package com.tlat.service.Impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tlat.Dto.UserDto;
import com.tlat.Entity.Role;
import com.tlat.Entity.User;
import com.tlat.Repository.RoleRepository;
import com.tlat.Repository.UserRepository;
import com.tlat.service.UserService;

import jakarta.persistence.EntityNotFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public void saveUser(UserDto userDto) {
    User user = new User();
    user.setName(userDto.getFirstName() + " " + userDto.getLastName());
    user.setEmail(userDto.getEmail());
    // Use email as username if not provided
    user.setUsername(userDto.getUsername() != null ? userDto.getUsername() : userDto.getEmail());
    user.setPassword(passwordEncoder.encode(userDto.getPassword()));
    user.setAge(userDto.getAge());
    user.setPhone(userDto.getPhone());
    user.setGender(userDto.getGender());
    user.setAddress(userDto.getAddress());
    
    Role role = roleRepository.findByName("ROLE_USER");
    if (role == null) {
        role = roleRepository.save(new Role(null, "ROLE_USER", null));
    }
    user.setRoles(Arrays.asList(role));
    userRepository.save(user);
}

@Override
public void editUser(UserDto updatedUserDto, Long userId) {
    User existingUser = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
    
    existingUser.setName(updatedUserDto.getFirstName() + " " + updatedUserDto.getLastName());
    existingUser.setAge(updatedUserDto.getAge());
    existingUser.setPhone(updatedUserDto.getPhone());
    existingUser.setGender(updatedUserDto.getGender());
    existingUser.setAddress(updatedUserDto.getAddress());
    
    // Update email and username if provided
    if (updatedUserDto.getEmail() != null && !updatedUserDto.getEmail().isEmpty()) {
        existingUser.setEmail(updatedUserDto.getEmail());
        existingUser.setUsername(updatedUserDto.getEmail()); // Username is same as email for login
    }
    
    // Update password if provided
    if (updatedUserDto.getPassword() != null && !updatedUserDto.getPassword().isEmpty()) {
        existingUser.setPassword(passwordEncoder.encode(updatedUserDto.getPassword()));
    }
    
    // როლის განახლება თუ მოწოდებულია და მომხმარებელი ადმინისტრატორია
    if (updatedUserDto.getRole() != null && !updatedUserDto.getRole().isEmpty()) {
        Role newRole = roleRepository.findByName(updatedUserDto.getRole());
        if (newRole != null) {
            existingUser.getRoles().clear();
            existingUser.getRoles().add(newRole);
        }
    }
    
    userRepository.save(existingUser);
}

    public void deleteUserById(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        userOptional.ifPresent(user -> {
            user.getRoles().clear();
            userRepository.delete(user);
        });
    }

    public boolean doesUserExist(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        return userOptional.isPresent();
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public UserDto findUserById(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if(userOptional.isPresent()){
            return mapToUserDto(userOptional.get());
        }
        return null;
    }

    @Override
    public List<UserDto> findAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map((user) -> mapToUserDto(user))
                .collect(Collectors.toList());
    }

    private UserDto mapToUserDto(User user){
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        String[] str = user.getName().split(" ");
        userDto.setFirstName(str[0]);
        userDto.setLastName(str.length > 1 ? str[1] : "");
        userDto.setEmail(user.getEmail());
        userDto.setAge(user.getAge());
        userDto.setPhone(user.getPhone());
        userDto.setGender(user.getGender());
        userDto.setAddress(user.getAddress());
        userDto.setRole(user.getRoles().get(0).getName());
        userDto.setAvatarPath(user.getAvatarPath());
        return userDto;
    }
    
    @Override
    public void updateUserAvatar(Long userId, String avatarPath) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setAvatarPath(avatarPath);
        userRepository.save(user);
    }

}