package id.ac.ui.cs.advprog.kki.json;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(scanBasePackages = "id.ac.ui.cs.advprog.kki.json")
public class JsonApplication {
    public static void main(String[] args) {
        SpringApplication.run(JsonApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}