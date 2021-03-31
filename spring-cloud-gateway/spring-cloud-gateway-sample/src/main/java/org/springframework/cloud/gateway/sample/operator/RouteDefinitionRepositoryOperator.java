package org.springframework.cloud.gateway.sample.operator;

import org.springframework.cloud.gateway.route.RouteDefinition;
import reactor.core.publisher.Flux;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @Date 2021/3/31
 */
public interface RouteDefinitionRepositoryOperator {

	Flux<RouteDefinition> getRouteDefinitions();

}
