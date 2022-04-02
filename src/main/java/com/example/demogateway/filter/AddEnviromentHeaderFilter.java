package com.example.demogateway.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class AddEnviromentHeaderFilter implements GlobalFilter, Ordered {
	
	static final Log log = LogFactory.getLog(AddEnviromentHeaderFilter.class);
	
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String host = exchange.getRequest().getURI().getHost();
        ServerHttpRequest newRequest = exchange.getRequest().mutate()
                .header("env", host)
                .build();
        log.info("增加header env=" + host);
		return chain.filter(exchange.mutate().request(newRequest).build());
	}

	@Override
	public int getOrder() {
		return 10149;
	}

}
