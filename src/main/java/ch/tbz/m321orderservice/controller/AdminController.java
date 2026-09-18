package ch.tbz.m321orderservice.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping
    public AdminInfo admin(@AuthenticationPrincipal Jwt jwt) {
        return new AdminInfo(
                jwt.getSubject(),
                jwt.getClaimAsString("preferred_username"),
                "ADMIN access granted");
    }

    public record AdminInfo(String subject, String username, String message) {
    }
}
