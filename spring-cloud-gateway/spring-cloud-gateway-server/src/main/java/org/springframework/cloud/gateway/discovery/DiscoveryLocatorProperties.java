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

package org.springframework.cloud.gateway.discovery;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.core.style.ToStringCreator;

@ConfigurationProperties("spring.cloud.gateway.discovery.locator")
public class DiscoveryLocatorProperties {

	//开启标识，默认关闭
	private boolean enabled = false;

	/**
	 * 路由ID前缀，默认为DiscoveryClient的类名称 {@link org.springframework.cloud.client.discovery.DiscoveryClient}
	 */
	//路由ID前缀，默认为
	private String routeIdPrefix;

	//是否使用SpEL表达式
	private String includeExpression = "true";

	//用来创建路由Route的uri表达式，最终会被解析为类似uri=lb://user-service，可覆盖
	private String urlExpression = "'lb://'+serviceId";

	/**
	 * Option to lower case serviceId in predicates and filters, defaults to false. Useful
	 * with eureka when it automatically uppercases serviceId. so MYSERIVCE, would match
	 * /myservice/**
	 */
	private boolean lowerCaseServiceId = false;

	private List<PredicateDefinition> predicates = new ArrayList<>();

	private List<FilterDefinition> filters = new ArrayList<>();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getRouteIdPrefix() {
		return routeIdPrefix;
	}

	public void setRouteIdPrefix(String routeIdPrefix) {
		this.routeIdPrefix = routeIdPrefix;
	}

	public String getIncludeExpression() {
		return includeExpression;
	}

	public void setIncludeExpression(String includeExpression) {
		this.includeExpression = includeExpression;
	}

	public String getUrlExpression() {
		return urlExpression;
	}

	public void setUrlExpression(String urlExpression) {
		this.urlExpression = urlExpression;
	}

	public boolean isLowerCaseServiceId() {
		return lowerCaseServiceId;
	}

	public void setLowerCaseServiceId(boolean lowerCaseServiceId) {
		this.lowerCaseServiceId = lowerCaseServiceId;
	}

	public List<PredicateDefinition> getPredicates() {
		return predicates;
	}

	public void setPredicates(List<PredicateDefinition> predicates) {
		this.predicates = predicates;
	}

	public List<FilterDefinition> getFilters() {
		return filters;
	}

	public void setFilters(List<FilterDefinition> filters) {
		this.filters = filters;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("enabled", enabled)
				.append("routeIdPrefix", routeIdPrefix)
				.append("includeExpression", includeExpression)
				.append("urlExpression", urlExpression)
				.append("lowerCaseServiceId", lowerCaseServiceId)
				.append("predicates", predicates).append("filters", filters).toString();
	}

}
