package org.springframework.cloud.gateway.sample.custom.repository.operator;

import com.alibaba.fastjson.JSONArray;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.sample.custom.repository.route.RedisRouteDefinitionRepository;
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
public class RedisRouteDefinitionRepositoryOperator implements RouteDefinitionRepositoryOperator {

	private final String REDIS_ROUTE_ID_PREFIX = "route-*";

	private StringRedisTemplate redisTemplate;

	public RedisRouteDefinitionRepositoryOperator(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}


	@Override
	public Flux<RouteDefinition> getRouteDefinitions() {
		//获取指定前缀的RedisKey。Redis的数据结构使用Hash，value的结构为predicates和filters，
		//predicates数据结构JsonArray，可配置多个
		//  由于PredicateDefinition的构造方法支持传入类似Path=/api/hello这种格式的参数，并会自动封装为name和args，因此我们取巧可以在Redis中存储如下结构
		// 		如：["Path=/api/hello","BlackRemoteAddr=172.17.30.1/18,172.17.31.1/18"]，表示PathRoutePredicateFactory和BlackRemoteAddrRoutePredicateFactory
		//filters与predicates一样
		return Flux.fromStream(redisTemplate.keys(REDIS_ROUTE_ID_PREFIX).parallelStream().map(routeId -> {
			RouteDefinition routeDefinition = new RouteDefinition();
			//以RedisKey作为RouteID
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
			//遍历predicates，创建RouteDefinition，并添加到RouteDefinition中
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
			//遍历predicates，创建RouteDefinition，并添加到RouteDefinition中
			FilterDefinition filterDefinition = new FilterDefinition((String) filter);
			routeDefinition.getFilters().add(filterDefinition);
		});
	}
}
