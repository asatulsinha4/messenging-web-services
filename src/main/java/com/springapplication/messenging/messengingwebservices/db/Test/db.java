package com.springapplication.messenging.messengingwebservices.db.Test;

import java.util.HashMap;
import java.util.HashSet;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class db {
    public static HashSet<String> AllUsers;
    public static HashMap<String, HashSet<String>> FriendRequest;
    public static HashMap<String, HashSet<String>> Friends;

    @PostConstruct
    public void init(){
        AllUsers = new HashSet<>();
        FriendRequest = new HashMap<>();
        Friends = new HashMap<>();
    }
    
}
