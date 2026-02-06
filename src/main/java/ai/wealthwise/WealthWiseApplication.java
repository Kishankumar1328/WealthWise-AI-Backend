package ai.wealthwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import org.springframework.scheduling.annotation.EnableAsync;

/**
 * WealthWise AI - Main Application Entry Point
 * 
 * Production-ready AI-Powered Personal Finance Management Platform
 * 
 * @author Senior Software Architect
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
// @EnableCaching
@EnableJpaAuditing
public class WealthWiseApplication {

    public static void main(String[] args) {
        SpringApplication.run(WealthWiseApplication.class, args);
        System.out.println("\n" +
                "==============================================\n" +
                "  WealthWise AI Backend - Started Successfully\n" +
                "  API Documentation: http://localhost:5000/api/swagger-ui.html\n" +
                "  Health Check: http://localhost:5000/api/actuator/health\n" +
                "==============================================\n");
    }
}
