package com.springapplication.messenging.config;

import com.springapplication.messenging.messengingwebservices.services.AdminUserAuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class AdminAuthConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private AdminUserAuthService adminUserAuthService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
    }
    
    
}
