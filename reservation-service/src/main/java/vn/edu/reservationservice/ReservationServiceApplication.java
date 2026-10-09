package vn.edu.reservationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class ReservationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReservationServiceApplication.class, args);
    }
}