package com.rnp.cremer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
public class CremerApplication {

    @PostConstruct
    public void init() {
        // Configurar zona horaria para toda la aplicación
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"));
        System.out.println("✅ Timezone configurado: " + TimeZone.getDefault().getID());
    }

    public static void main(String[] args) {
        SpringApplication.run(CremerApplication.class, args);
    }
}