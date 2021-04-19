package org.springframework.cloud.gateway.sample.operator;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.sample.route.RedisRouteDefinitionRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Description：用来获取Redis中的RouteDefinition 并保存到{@link RedisRouteDefinitionRepository}
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @Date 2021/3/31
 */
public class RedidRouteDefinitionRepositoryOperator implements RouteDefinitionRepositoryOperator {

	private final String REDIS_ROUTE_ID_PREFIX = "route-*";

	private StringRedisTemplate redisTemplate;

	public RedidRouteDefinitionRepositoryOperator(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}


	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
		return Flux.fromStream(redisTemplate.keys(REDIS_ROUTE_ID_PREFIX).parallelStream().map(routeId -> {
			RouteDefinition routeDefinition = new RouteDefinition();
			routeDefinition.setId(routeId);
			Map<Object, Object> entries = redisTemplate.opsForHash().entries(routeId);
			String uri = (String) entries.get("uri");
			try {
				routeDefinition.setUri(new URI(uri));
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			//初始化PredicateDefinition，并添加到RouteDefinition中
			initPredicate(routeDefinition, entries);

			//初始化FilterDefinition，并添加到RouteDefinition中
			initFilter(routeDefinition, entries);
			System.out.println("RedisRouteDefinition：" + JSONObject.toJSONString(routeDefinition));
			return routeDefinition;
		}));
	}

	private void initPredicate(RouteDefinition routeDefinition, Map<Object, Object> entries) {
		Object predicates = entries.get("predicates");
		if (predicates == null) {
			return;
		}
		JSONArray predicateArry = JSONArray.parseArray((String) predicates);
		predicateArry.parallelStream().forEach(predicate -> {
			PredicateDefinition predicateDefinition = new PredicateDefinition((String) predicate);
			routeDefinition.getPredicates().add(predicateDefinition);
		});
	}

	private void initFilter(RouteDefinition routeDefinition, Map<Object, Object> entries) {
		Object filters = entries.get("filters");
		if (filters == null) {
			return;
		}
		JSONArray predicateArry = JSONArray.parseArray((String) filters);
		predicateArry.parallelStream().forEach(filter -> {
			FilterDefinition filterDefinition = new FilterDefinition((String) filter);
			routeDefinition.getFilters().add(filterDefinition);
		});
	}
}
