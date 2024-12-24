package com.springapplication.messenging.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ExecuteCommands {

    public List<String> executeCommandOnce (String cmd) throws Exception{
        List<String> output = new ArrayList<>();
        log.info("Command received to execute:  + cmd");
        String[] command = {"/bin/sh", "-c", cmd};
        Process p;
        try{
            p = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            output = reader.lines().map(line -> line.trim()).collect(Collectors.toList());
            p.waitFor();
            log.info("Command executed successfully, output contains " + output.size() + " lines");
        }catch(Exception exception){
            log.error("Command execution failed: \n" + exception.getStackTrace());
            throw exception;
        }
        return output;
    }

    public List<String> executeMultipleCommands(List<String> commands, String file_path) throws Exception{
        List<String> output = new ArrayList<>();
        ProcessBuilder builder = new ProcessBuilder("/bin/bash");
        log.info("Multiple commands received for execution: \n" + commands.stream());
        builder.redirectErrorStream(true);
        if(file_path.length() > 0){
            File file = new File(file_path);
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(file));
        }else{
            builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        }
        try {
            Process p = builder.start();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            commands.forEach(cmd -> {
                try {
                    writer.write("echo");
                    writer.newLine();
                    writer.write("echo");
                    writer.newLine();
                    writer.write("echo \">>" + cmd + "\"");
                    writer.newLine();
                    writer.write("echo");
                    writer.newLine();
                    writer.write(cmd);
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    log.error("Error occured while executing commands: \n" + e.getStackTrace());
                }
            });
            try {
                writer.write("exit");
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                log.error("Error occurred: " + e.getStackTrace());
            }
            p.waitFor();
            output = reader.lines().collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error occurred: " + e.getStackTrace());
            throw e;
        }
        return output;
    }
    
}
