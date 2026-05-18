package com.tlat.controller;

import com.tlat.entity.User;
import com.tlat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final UserService userService;

    @ModelAttribute("currentUser")
    public User currentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return userService.findUserByEmail(principal.getName());
    }
}