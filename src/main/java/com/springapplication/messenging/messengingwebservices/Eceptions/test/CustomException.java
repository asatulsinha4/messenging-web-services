package com.springapplication.messenging.messengingwebservices.Eceptions.test;

public class CustomException extends Exception {
    public CustomException(String message){
        super(message);
    }
    public CustomException(String message, Throwable err){
        super(message, err);
    }
    
}
