package com.example.order.config;

import com.example.order.repository.FileOrderRepository;
import com.example.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Paths;

@Configuration
@EnableScheduling
public class AppConfig {

    @Bean
    public OrderRepository orderRepository(@Value("${order.repository.data-dir:data/orders}") String dataDir) {
        return new FileOrderRepository(Paths.get(dataDir));
    }
}
