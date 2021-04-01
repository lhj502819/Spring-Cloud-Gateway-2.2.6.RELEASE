package org.springframework.cloud.gateway.sample.custom.repository.operator;

import org.springframework.cloud.gateway.route.RouteDefinition;
import reactor.core.publisher.Flux;

/**
 * 定义从不同数据源获取RouteDefinition的抽象
 * @author li.hongjian
 * @email lhj502819@163.com
 * @Date 2021/3/31
 */
public interface RouteDefinitionRepositoryOperator {

	Flux<RouteDefinition> getRouteDefinitions();

}
