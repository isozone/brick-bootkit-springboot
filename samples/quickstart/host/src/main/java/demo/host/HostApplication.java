package demo.host;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class HostApplication {

    public static void main(String[] args) {
        SpringApplication.run(HostApplication.class, args);
    }
}
