package com.example.demogateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 通过配置中心动态路由
 * @author zhangyl
 *
 */
@SpringBootApplication
@EnableDiscoveryClient
public class DemoGatewayApp {
	
	public static void main(String[] args) {
		SpringApplication.run(DemoGatewayApp.class, args);
	}
	
}
