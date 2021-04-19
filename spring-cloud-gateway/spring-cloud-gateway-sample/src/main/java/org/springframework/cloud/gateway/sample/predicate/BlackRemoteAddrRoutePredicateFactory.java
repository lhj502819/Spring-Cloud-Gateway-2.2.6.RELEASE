package org.springframework.cloud.gateway.sample.predicate;

import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.handler.predicate.GatewayPredicate;
import org.springframework.cloud.gateway.support.ipresolver.RemoteAddressResolver;
import org.springframework.web.server.ServerWebExchange;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static org.springframework.cloud.gateway.support.ShortcutConfigurable.ShortcutType.GATHER_LIST;

/**
 * Description：黑名单Predicate
 * @author li.hongjian
 * @email lhj502819@163.com
 * @Date 2021/3/31
 */
public class BlackRemoteAddrRoutePredicateFactory
		extends AbstractRoutePredicateFactory<BlackRemoteAddrRoutePredicateFactory.Config> {

	public BlackRemoteAddrRoutePredicateFactory() {
		super(Config.class);
	}

	@NotNull
	private List<IpSubnetFilterRule> convert(List<String> values) {
		List<IpSubnetFilterRule> sources = new ArrayList<>();
		for (String arg : values) {
			addSource(sources, arg);
		}
		return sources;
	}

	/**
	 * 此方法需重写，用来创建Config使用
	 * @return
	 */
	@Override
	public ShortcutType shortcutType() {

		/**
		 * GATHER_LIST类型只能有一个shortcutField
		 * {@link this#shortcutFieldOrder()}
		 */
		return GATHER_LIST;
	}

	/**
	 * 配置中的value对应的字段
	 * 比如当前我们的Config中的字段就为sources
	 * @return
	 */
	@Override
	public List<String> shortcutFieldOrder() {
		return Collections.singletonList("sources");
	}


	@Override
	public Predicate<ServerWebExchange> apply(Config config) {
		/**
		 * IpSubnetFilterRule是Netty中定义的IP过滤规则
		 */
		//根据配置的sources生成对应规则
		List<IpSubnetFilterRule> sources = convert(config.sources);

		return new GatewayPredicate() {
			@Override
			public boolean test(ServerWebExchange exchange) {
				InetSocketAddress remoteAddress = config.remoteAddressResolver
						.resolve(exchange);
				if (remoteAddress != null && remoteAddress.getAddress() != null) {
					//只要符合任意一个规则就返回false，与RemoteAddrRoutePredicateFactory相反
					for (IpSubnetFilterRule source : sources) {
						if (source.matches(remoteAddress)) {
							return false;
						}
					}
				}
				//如果没有匹配所有规则，则通过
				return true;
			}
		};
	}

	private void addSource(List<IpSubnetFilterRule> sources, String source) {
		//判断是否配置了IP段，如果没有则默认为最大为32，如配置172.15.32.15，则被修改为172.15.32.15/32
		if (!source.contains("/")) { // no netmask, add default
			source = source + "/32";
		}
		//假设配置的为 172.15.32.15/18
		//根据'/'分割  [0]:172.15.32.15  [1]:18
		String[] ipAddressCidrPrefix = source.split("/", 2);
		String ipAddress = ipAddressCidrPrefix[0];
		int cidrPrefix = Integer.parseInt(ipAddressCidrPrefix[1]);

		//设置拒绝规则
		sources.add(
				new IpSubnetFilterRule(ipAddress, cidrPrefix, IpFilterRuleType.REJECT));
	}

	public static class Config{
		/**
		 * 可配置多个IP/IP段
		 */
		@NotEmpty
		private List<String> sources = new ArrayList<>();
		/**
		 * 用来解析客户端IP
		 */
		@NotNull
		private RemoteAddressResolver remoteAddressResolver = new RemoteAddressResolver() {
		};

		public List<String> getSources() {
			return sources;
		}

		public Config setSources(List<String> sources) {
			this.sources = sources;
			return this;
		}

		public Config setSources(String... sources) {
			this.sources = Arrays.asList(sources);
			return this;
		}

		public Config setRemoteAddressResolver(
				RemoteAddressResolver remoteAddressResolver) {
			this.remoteAddressResolver = remoteAddressResolver;
			return this;
		}
	}
}
