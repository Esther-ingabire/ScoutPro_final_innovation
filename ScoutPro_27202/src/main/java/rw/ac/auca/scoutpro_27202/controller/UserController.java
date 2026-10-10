package rw.ac.auca.scoutpro_27202.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import rw.ac.auca.scoutpro_27202.domain.User;
import rw.ac.auca.scoutpro_27202.dto.PageResponse;
import rw.ac.auca.scoutpro_27202.dto.RoleUpdateRequest;
import rw.ac.auca.scoutpro_27202.service.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public PageResponse<User> getAllUsers(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return userService.getAllUsers(page, size);
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable UUID id) {
        return userService.getUserById(id);
    }

    @PutMapping("/{id}/roles")
    public User updateRoles(@PathVariable UUID id,
                            @RequestBody RoleUpdateRequest request,
                            @AuthenticationPrincipal Jwt jwt) {
        // the admin's own id comes from their token, not from the request
        UUID currentAdminId = UUID.fromString(jwt.getClaimAsString("userId"));
        return userService.updateRoles(id, request.roles(), currentAdminId);
    }
}