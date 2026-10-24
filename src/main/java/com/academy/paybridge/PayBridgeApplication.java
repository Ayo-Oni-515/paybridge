package com.academy.paybridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // picks up FxProperties, RemittanceProperties, ...
public class PayBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayBridgeApplication.class, args);
    }
}
