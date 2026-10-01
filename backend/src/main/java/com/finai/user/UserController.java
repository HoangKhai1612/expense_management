package com.finai.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "The authenticated user's own profile")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Return the authenticated user's profile")
    public UserService.ProfileView me() {
        return userService.getProfile();
    }

    @PatchMapping("/me")
    @Operation(summary = "Update the authenticated user's profile")
    public UserService.ProfileView updateProfile(
            @Valid @RequestBody UserService.UpdateProfileRequest request) {
        return userService.updateProfile(request);
    }

    @PostMapping("/me/password")
    @Operation(summary = "Change the authenticated user's password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody UserService.ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
