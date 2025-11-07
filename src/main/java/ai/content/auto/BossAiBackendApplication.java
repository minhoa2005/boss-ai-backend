package ai.content.auto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BossAiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BossAiBackendApplication.class, args);
    }

}
