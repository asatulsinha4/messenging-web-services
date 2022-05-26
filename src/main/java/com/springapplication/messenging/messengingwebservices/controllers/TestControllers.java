package com.springapplication.messenging.messengingwebservices.controllers;

import com.springapplication.messenging.messengingwebservices.dto.test.CreateDTO;
import com.springapplication.messenging.messengingwebservices.dto.test.FriendRequestDTO;
import com.springapplication.messenging.messengingwebservices.dto.test.FriendsDTO;
import com.springapplication.messenging.messengingwebservices.services.Test.UsersService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.GetMapping;

/**
 * 
 * @author Atul Sinha
 */
@RestController
@RequestMapping("/test")
public class TestControllers {

    @Autowired
    private UsersService usersService;

    @PostMapping(path = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateDTO> createUser(@RequestBody String username){
        CreateDTO response = usersService.createUser(username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping(value = "/add/{id1}/{id2}")
    public ResponseEntity<FriendRequestDTO> sendFriendRequest(@PathVariable("id1") String user1, @PathVariable("id2") String user2){
        FriendRequestDTO response = usersService.requestResponse(user1, user2);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @GetMapping(value="/friends/{id1}")
    public ResponseEntity<FriendsDTO> friendsList(@PathVariable("id1") String username) {
        FriendsDTO response = usersService.getFriendList(username);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
    
    
}
