package com.example.demogateway.loadbalance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 对 service-mypro 服务做自定义负载均衡 
 * @author zhangyl
 *
 */
@LoadBalancerClient(name="service-mypro", configuration = CustomLoadBalancerConfiguration.class )
public class CustomLoadBalancerConfiguration {
	
	static final Log log = LogFactory.getLog(CustomLoadBalancerConfiguration.class);
	
	@Bean
	public ReactorLoadBalancer<ServiceInstance> myproReactorServiceInstanceLoadBalancer(Environment environment,
			LoadBalancerClientFactory loadBalancerClientFactory) {
		String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
		ObjectProvider<ServiceInstanceListSupplier> provider = loadBalancerClientFactory.getLazyProvider(name,
				ServiceInstanceListSupplier.class);
		return new MyLoadBalancer(provider, name);
	}
}
