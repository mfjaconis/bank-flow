package com.jaconis.bankflow.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AccountServiceApplication {
    static public void main(String[] args){
        SpringApplication.run(AccountServiceApplication.class, args);

        System.out.println("Hello Account Service!");
    }
}
