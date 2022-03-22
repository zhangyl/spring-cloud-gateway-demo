package com.example.demogateway.loadbalance;


import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.SelectedInstanceCallback;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import com.google.common.base.Optional;

import io.micrometer.core.instrument.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * 自定义负载均衡示例，从RoundRobinLoadBalancer抄过来的
 * @author zhangyl
 *
 */
public class MyLoadBalancer implements ReactorServiceInstanceLoadBalancer {

	private static final Log log = LogFactory.getLog(MyLoadBalancer.class);

	final AtomicInteger position;

	final String serviceId;

	ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

	public MyLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
			String serviceId) {
		this(serviceInstanceListSupplierProvider, serviceId, new Random().nextInt(1000));
	}

	public MyLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
			String serviceId, int seedPosition) {
		this.serviceId = serviceId;
		this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
		this.position = new AtomicInteger(seedPosition);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Mono<Response<ServiceInstance>> choose(Request request) {
		RequestDataContext ctx = (RequestDataContext)request.getContext();
		RequestData requestData = ctx.getClientRequest();
		URI uri = requestData.getUrl();
		//通过域名区分不同环境
		String host = uri.getHost();
		log.info("----------------------------------host="+ host);
		
		ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);
		Mono<List<ServiceInstance>> serviceList = supplier.get(request).next();
		Mono<Response<ServiceInstance>> result = serviceList.map(serviceInstances -> {
			Response<ServiceInstance> serviceInstanceResponse = getInstanceResponse(serviceInstances, host);
			if (supplier instanceof SelectedInstanceCallback && serviceInstanceResponse.hasServer()) {
				((SelectedInstanceCallback) supplier).selectedServiceInstance(serviceInstanceResponse.getServer());
			}
			return serviceInstanceResponse;
		});
		return result;
	}

	/**
	 * 通过自定义设置 metadata 标签路由，优先选择完全匹配到环境标签的服务，不然降级到基准稳定环境
	 * @param instances
	 * @param metaMatchTag
	 * @return
	 */
	private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances, String metaMatchTag) {
		if (instances.isEmpty()) {
			if (log.isWarnEnabled()) {
				log.warn("No servers available for service: " + serviceId);
			}
			return new EmptyResponse();
		}
		//默认稳定的基准服务
		List<ServiceInstance> defaultServerList = new ArrayList<>();
		//精确匹配环境tag的服务
		List<ServiceInstance> tagServerList = new ArrayList<>();
		for(ServiceInstance instance : instances) {
			Map<String, String> metaData = instance.getMetadata();
			log.info("instance=============" + instance.getHost() + ":" + instance.getPort() + " ================ " + metaData);
			String env = Optional.fromNullable(metaData.get("env")).or("");
			if(StringUtils.isBlank(env)) {
				defaultServerList.add(instance);
				continue;
			}
			if(StringUtils.isNotBlank(metaMatchTag) && env.trim().equals(metaMatchTag.trim())) {
				tagServerList.add(instance);
				continue;
			}
		}
		
		int pos = Math.abs(this.position.incrementAndGet());
		//优先选择完全匹配到环境标签的服务
		if(tagServerList.size() > 0) {
			ServiceInstance instance = instances.get(pos % tagServerList.size());
			return new DefaultResponse(instance);
		}
		//路由降级到稳定的基准环境
		if(defaultServerList.size() > 0) {
			ServiceInstance instance = instances.get(pos % defaultServerList.size());
			return new DefaultResponse(instance);
		}
		//基准环境也没有，随便选择一个存在的服务吧
		ServiceInstance instance = instances.get(pos % instances.size());
		return new DefaultResponse(instance);
	}

}
