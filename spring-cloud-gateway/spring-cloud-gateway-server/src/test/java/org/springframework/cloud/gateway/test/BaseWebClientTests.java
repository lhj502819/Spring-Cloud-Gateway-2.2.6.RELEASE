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

package org.springframework.cloud.gateway.test;

import java.time.Duration;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import reactor.core.publisher.Mono;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_HANDLER_MAPPER_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * @author Spencer Gibb
 */
public class BaseWebClientTests {

	protected static final String HANDLER_MAPPER_HEADER = "X-Gateway-Handler-Mapper-Class";

	protected static final String ROUTE_ID_HEADER = "X-Gateway-RouteDefinition-Id";

	protected static final Duration DURATION = Duration.ofSeconds(5);

	@LocalServerPort
	protected int port = 0;

	protected WebTestClient testClient;

	protected WebClient webClient;

	protected String baseUri;

	@Before
	public void setup() throws Exception {
		setup(new ReactorClientHttpConnector(), "http://localhost:" + port);
	}

	protected void setup(ClientHttpConnector httpConnector, String baseUri) {
		this.baseUri = baseUri;
		this.webClient = WebClient.builder().clientConnector(httpConnector)
				.baseUrl(this.baseUri).build();
		this.testClient = WebTestClient.bindToServer(httpConnector).baseUrl(this.baseUri)
				.build();
	}

	@Configuration(proxyBeanMethods = false)
	@RibbonClients({
			@RibbonClient(name = "testservice", configuration = TestRibbonConfig.class) })
	@Import(PermitAllSecurityConfiguration.class)
	public static class DefaultTestConfig {

		private static final Log log = LogFactory.getLog(DefaultTestConfig.class);

		@Bean
		public HttpBinCompatibleController httpBinController() {
			return new HttpBinCompatibleController();
		}

		@Bean
		public RecursiveHttpbinFilter recursiveHttpbinFilter() {
			return new RecursiveHttpbinFilter();
		}

		@Bean
		@Order(500)
		public GlobalFilter modifyResponseFilter() {
			return (exchange, chain) -> {
				log.info("modifyResponseFilter start");
				String value = exchange.getAttributeOrDefault(GATEWAY_HANDLER_MAPPER_ATTR,
						"N/A");
				if (!exchange.getResponse().isCommitted()) {
					exchange.getResponse().getHeaders().add(HANDLER_MAPPER_HEADER, value);
				}
				Route route = exchange.getAttributeOrDefault(GATEWAY_ROUTE_ATTR, null);
				if (route != null) {
					if (!exchange.getResponse().isCommitted()) {
						exchange.getResponse().getHeaders().add(ROUTE_ID_HEADER,
								route.getId());
					}
				}
				return chain.filter(exchange);
			};
		}

	}

	public static class RecursiveHttpbinFilter implements GlobalFilter {

		@Override
		public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
			if (exchange.getRequest().getPath().toString().contains("httpbin/httpbin")) {
				return Mono
						.error(new IllegalStateException("recursive call to /httpbin"));
			}
			return chain.filter(exchange);
		}

	}

	@EnableAutoConfiguration
	@SpringBootConfiguration
	@Import(DefaultTestConfig.class)
	public static class MainConfig {

	}

	protected static class TestRibbonConfig {

		@LocalServerPort
		protected int port = 0;

		@Bean
		public ServerList<Server> ribbonServerList() {
			return new StaticServerList<>(new Server("localhost", this.port));
		}

	}

}
