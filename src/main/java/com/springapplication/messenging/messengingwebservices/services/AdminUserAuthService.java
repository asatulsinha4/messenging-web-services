package com.springapplication.messenging.messengingwebservices.services;


import com.springapplication.messenging.messengingwebservices.db.TestDb;
import com.springapplication.messenging.models.AdminUser;
import com.springapplication.messenging.util.PasswordUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AdminUserAuthService {

    @Autowired
    private PasswordUtil passwordUtil;

    public void changePassword(String oldPassword, String newPassword, String username) {
        log.info("Request recieved to change password for user: " + username);
        try {
            if (TestDb.AllAdminUsers.get(username).getPassword().equals(passwordUtil.encode(oldPassword, username))) {
                TestDb.AllAdminUsers.get(username).setPassword(passwordUtil.encode(newPassword, username));
                log.info("Password for user " + username + " changed successfully");
            } else {
                log.info("Password mismatch for user: " + username + " while changing the password");
            }
        } catch (Exception exception) {
            log.error("Error occured while changing password for user: " + username + " :: " + exception.getMessage());
        }
    }

    public void createUser(AdminUser user) {
        log.info("request received to create user: " + user.getUsername());
        if (userExists(user.getUsername())) {
            log.info(user.getUsername() + " already exists");
        } else {
            try {
                user.setPassword(passwordUtil.encode(user.getPassword(), user.getUsername()));
                TestDb.AllAdminUsers.put(user.getUsername(), user);
                log.info(user.getUsername() + " created successfully");
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Error occurred while creating user: " + user.getUsername() + " :: " + e.getMessage());
            }
        }
    }

    public void deleteUser(String username) {
        log.info("request received to delete user: " + username);
        if (TestDb.AllAdminUsers.remove(username) != null)
            log.info(username + " removed successfull");
        else
            log.info("user not found");
    }

    public void updateUser(AdminUser user) {
        log.info("request received to update user: " + user.getUsername());
        TestDb.AllAdminUsers.put(user.getUsername(), user);
        log.info(user.getUsername() + " updated successfull");
    }

    public boolean userExists(String username) {
        if (TestDb.AllAdminUsers.containsKey(username))
            return true;
        return false;
    }

    public AdminUser loadUserByUsername(String username) throws UsernameNotFoundException {
        return TestDb.AllAdminUsers.get(username);
    }

    public boolean userAuth(AdminUser user) throws Exception {
        try{
            if (userExists(user.getUsername())){
                if (TestDb.AllAdminUsers.get(user.getUsername()).getPassword()
                        .equals(passwordUtil.encode(user.getPassword(), user.getUsername()))) {
                    log.info("user "+user.getUsername()+" authenticated successfully");
                    return true;
                    }
            }
            log.warn("Invalid user credentials found for user: "+user.getUsername());
            return false;
        }catch(Exception exception){
            log.error("Error occurred while authenticating for user: "+user.getUsername()+" :: "+exception.getMessage());
            exception.printStackTrace();
            throw exception;
        }
    }

}