package com.springapplication.messenging.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PasswordUtil {

    public String encode(String password, String username) throws NoSuchAlgorithmException{
        try{
            MessageDigest md5encoder = MessageDigest.getInstance("MD5");
            md5encoder.update(password.getBytes());
            String encodedPassword = "";
            for(byte x: md5encoder.digest()){
                String hex = Integer.toHexString(0xff & x);
                if(hex.length() == 1) hex = "0" + hex;
                encodedPassword = encodedPassword + hex;
            }
            encodedPassword = encodedPassword + Base64.getUrlEncoder().encodeToString(username.getBytes());
            return encodedPassword;
        }catch (Exception exception){
            log.error("Error while encoding password :: "+exception.getMessage());
            return "";
        }
    }

    public String md5encoder(String password){
        return DigestUtils.md5DigestAsHex(password.getBytes());
    }
    
}