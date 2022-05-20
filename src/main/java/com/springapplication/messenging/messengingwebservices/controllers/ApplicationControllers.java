package com.springapplication.messenging.messengingwebservices.controllers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping
@RestController
public class ApplicationControllers {

    @GetMapping(value = "/")
    public String helloWorld(){
        return "Hello World!";
    }
    
}
