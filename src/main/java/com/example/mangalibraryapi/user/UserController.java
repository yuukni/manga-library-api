package com.example.mangalibraryapi.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/user")
public class UserController {

    @Autowired
    private UserService userService;

    // Already in /api/registration
    /*@PostMapping
    public User registerUser(@RequestBody User user) {
        return userService.createUser(user);
    }*/
}