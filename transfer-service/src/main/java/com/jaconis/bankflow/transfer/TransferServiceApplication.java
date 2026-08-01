package com.jaconis.bankflow.transfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TransferServiceApplication {
    static public void main(String[] args){
        SpringApplication.run(TransferServiceApplication.class, args);
            System.out.println("Hello Transfer Service!");
    }
}
