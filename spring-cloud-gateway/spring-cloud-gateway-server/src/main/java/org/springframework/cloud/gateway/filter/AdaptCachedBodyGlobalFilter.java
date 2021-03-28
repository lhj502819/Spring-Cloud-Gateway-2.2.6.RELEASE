/*
 * Copyright 2013-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gateway.filter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.event.EnableBodyCachingEvent;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

public class AdaptCachedBodyGlobalFilter
		implements GlobalFilter, Ordered, ApplicationListener<EnableBodyCachingEvent> {

	private ConcurrentMap<String, Boolean> routesToCache = new ConcurrentHashMap<>();

	/**
	 * Cached request body key.
	 */
	@Deprecated
	public static final String CACHED_REQUEST_BODY_KEY = CACHED_REQUEST_BODY_ATTR;

	/**
	 * 当我们配置了RetryGatewayFilterFactory重试时，会在执行重试逻辑时发布EnableBodyCachingEvent，此处会监听到该事件
	 * @param event
	 */
	@Override
	public void onApplicationEvent(EnableBodyCachingEvent event) {
		this.routesToCache.putIfAbsent(event.getRouteId(), true);
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		// the cached ServerHttpRequest is used when the ServerWebExchange can not be
		// mutated, for example, during a predicate where the body is read, but still
		// needs to be cached.
		//从上下文获取CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR，CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR通过名称可以看出来就是请求的封装
		//当使用了ReadBodyRoutePredicateFactory时，test时会将请求body放入上下文中，此处的缓存主要是为了后边不用再序列化
		//如果不为空，表示请求和请求体都已经缓存了，则通过缓存的request构建一个上下文请求
		ServerHttpRequest cachedRequest = exchange
				.getAttributeOrDefault(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR, null);
		if (cachedRequest != null) {
			exchange.getAttributes().remove(CACHED_SERVER_HTTP_REQUEST_DECORATOR_ATTR);
			return chain.filter(exchange.mutate().request(cachedRequest).build());
		}

		//如果上边没有从上下文中获取到缓存，则获取CACHED_REQUEST_BODY_ATTR
		// CACHED_REQUEST_BODY_ATTR是请求体的缓存，此处的缓存可能是想让我们通过自定义Predicate或者Filter的方式在此Filter之前将body先序列化缓存
		DataBuffer body = exchange.getAttributeOrDefault(CACHED_REQUEST_BODY_ATTR, null);
		Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);

		//此处判断body是否为空或者routesToCache是否包含当前路由ID，表示当前请求已经被缓存过
		//routesToCache在#onApplicationEvent中可能会put值
		if (body != null || !this.routesToCache.containsKey(route.getId())) {
			return chain.filter(exchange);
		}
		//如果上边的条件都不满足，则会将当前请求Body放到缓存中
		return ServerWebExchangeUtils.cacheRequestBody(exchange, (serverHttpRequest) -> {
			// don't mutate and build if same request object
			//如果是同一个请求，则直接执行Filter逻辑
			if (serverHttpRequest == exchange.getRequest()) {
				return chain.filter(exchange);
			}
			//否则，通过新的请求构建一个请求上下文
			return chain.filter(exchange.mutate().request(serverHttpRequest).build());
		});
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 1000;
	}

}
