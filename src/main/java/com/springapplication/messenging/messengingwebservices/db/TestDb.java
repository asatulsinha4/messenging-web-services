package com.springapplication.messenging.messengingwebservices.db;

import java.util.HashMap;
import java.util.HashSet;

import javax.annotation.PostConstruct;

import com.springapplication.messenging.models.AdminUser;

import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

/**
 * A test DB implementation with hashmap and hashset
 * 
 * @author Atul Sinha
 */
@Slf4j
@Repository
public class TestDb {
    public static HashSet<String> AllUsers;
    public static HashMap<String, HashSet<String>> FriendRequest;
    public static HashMap<String, HashSet<String>> Friends;

    public static HashMap<String, AdminUser> AllAdminUsers;

    @PostConstruct
    public void init(){
        AllUsers = new HashSet<>();
        FriendRequest = new HashMap<>();
        Friends = new HashMap<>();
        AllAdminUsers = new HashMap<>();
        log.info("AllUsers, FriendRequest, Friends, AllAdminUsers are active");
    }
    
}
