package com.springapplication.messenging.util;

import java.io.File;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Printer {

    public void printList(List<String> list){
        for(String line: list){
            System.out.println(line);
        }
    }

    public void printList(List<String> list, File file) throws Exception{
        if(file.canWrite()){
            //do something
        }else{
            log.error("Something wrong happend");
            throw new Exception("Something wrong happend");
        }
    }
    
}
