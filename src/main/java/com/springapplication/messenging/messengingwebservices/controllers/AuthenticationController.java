package com.springapplication.messenging.messengingwebservices.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.springapplication.messenging.models.AdminUser;
import com.springapplication.messenging.util.JwtUtil;
import com.springapplication.messenging.messengingwebservices.Exceptions.CustomException;
import com.springapplication.messenging.messengingwebservices.services.AdminUserAuthService;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    @Autowired
    private AdminUserAuthService adminUserAuthService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping(value = "/")
    public ResponseEntity<String> helloWorld() {
        return ResponseEntity.status(HttpStatus.OK).body("Hello World!");
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> authenticationResponse(@RequestBody AdminUser userDetails,
            @RequestHeader Map<String, Object> header) throws Exception {
        try {
            boolean authenticated = adminUserAuthService.userAuth(userDetails);
            if (!authenticated) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect User Credentials");
            }
        } catch (Exception exception) {
            throw new CustomException("Authentication failed", exception);
        }
        final String jwt = jwtUtil.generateToken(userDetails, new HashMap<>(), header);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(jwt);
    }

}
