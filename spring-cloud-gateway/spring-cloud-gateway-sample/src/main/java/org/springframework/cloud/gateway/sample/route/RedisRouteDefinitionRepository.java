package org.springframework.cloud.gateway.sample.route;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.cloud.gateway.sample.operator.RedidRouteDefinitionRepositoryOperator;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

import static java.util.Collections.synchronizedMap;

/**
 * Description：基于Redis作为RouteDefinition Repository
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @Date 2021/3/31
 */
public class RedisRouteDefinitionRepository implements RouteDefinitionRepository{

	private final Map<String, RouteDefinition> routes = synchronizedMap(
			new LinkedHashMap<String, RouteDefinition>());

	private RedidRouteDefinitionRepositoryOperator redidRouteDefinitionOperator;

	public RedisRouteDefinitionRepository(RedidRouteDefinitionRepositoryOperator redidRouteDefinitionOperator) {
		this.redidRouteDefinitionOperator = redidRouteDefinitionOperator;
	}

	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
		redidRouteDefinitionOperator.getRouteDefinitions().flatMap(r -> save(Mono.just(r))).subscribe();
		return Flux.fromIterable(routes.values());
	}

	@Override
	public Mono<Void> save(Mono<RouteDefinition> route) {
		return route.flatMap(r -> {
			if (StringUtils.isEmpty(r.getId())) {
				return Mono.error(new IllegalArgumentException("id may not be empty"));
			}
			routes.put(r.getId(), r);
			return Mono.empty();
		});
	}

	@Override
	public Mono<Void> delete(Mono<String> routeId) {
		return routeId.flatMap(id -> {
			if (routes.containsKey(id)) {
				routes.remove(id);
				return Mono.empty();
			}
			return Mono.defer(() -> Mono.error(
					new NotFoundException("RouteDefinition not found: " + routeId)));
		});
	}
}
