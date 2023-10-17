package it.pagopa.pn.pecmock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan
public class PecMockApplication {

    public static void main(String[] args) {
        SpringApplication.run(PecMockApplication.class, args);
    }

}