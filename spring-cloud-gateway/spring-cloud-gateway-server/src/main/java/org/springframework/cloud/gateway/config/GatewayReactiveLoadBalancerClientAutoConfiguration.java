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

package org.springframework.cloud.gateway.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.gateway.config.conditional.ConditionalOnEnabledGlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;

/**
 * AutoConfiguration for {@link ReactiveLoadBalancerClientFilter}.
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ LoadBalancerClient.class, ReactiveLoadBalancer.class,
		LoadBalancerAutoConfiguration.class, DispatcherHandler.class })
@AutoConfigureBefore(GatewayLoadBalancerClientAutoConfiguration.class)
@AutoConfigureAfter(LoadBalancerAutoConfiguration.class)
@EnableConfigurationProperties(LoadBalancerProperties.class)
public class GatewayReactiveLoadBalancerClientAutoConfiguration {

	@Bean
	@ConditionalOnBean(LoadBalancerClientFactory.class)
	@ConditionalOnMissingBean(ReactiveLoadBalancerClientFilter.class)
	@Conditional(OnNoRibbonDefaultCondition.class)
	@ConditionalOnEnabledGlobalFilter
	public ReactiveLoadBalancerClientFilter gatewayLoadBalancerClientFilter(
			LoadBalancerClientFactory clientFactory, LoadBalancerProperties properties) {
		return new ReactiveLoadBalancerClientFilter(clientFactory, properties);
	}

	private static final class OnNoRibbonDefaultCondition extends AnyNestedCondition {

		private OnNoRibbonDefaultCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(value = "spring.cloud.loadbalancer.ribbon.enabled",
				havingValue = "false")
		static class RibbonNotEnabled {

		}

		@ConditionalOnMissingClass("org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient")
		static class RibbonLoadBalancerNotPresent {

		}

	}

}
