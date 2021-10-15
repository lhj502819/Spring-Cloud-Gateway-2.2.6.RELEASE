package org.springframework.cloud.gateway.sample.custom.repository.route;

import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 路由定时刷新
 *
 * @author li.hongjian
 * @email lhj502819@163.com
 * @Date 2021/4/1
 */
public class RedisRouteDefinitionWatch implements ApplicationEventPublisherAware, SmartLifecycle {

	private final TaskScheduler taskScheduler = getTaskScheduler();

	private final AtomicLong redisWatchIndex = new AtomicLong(0);

	private final AtomicBoolean running = new AtomicBoolean(false);

	private ApplicationEventPublisher publisher;

	private ScheduledFuture<?> watchFuture;

	private static ThreadPoolTaskScheduler getTaskScheduler() {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setBeanName("Redis-Watch-Task-Scheduler");
		taskScheduler.initialize();
		return taskScheduler;
	}


	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public void start() {
		if (this.running.compareAndSet(false, true)) {
			this.watchFuture = this.taskScheduler.scheduleWithFixedDelay(
					this::redisServicesWatch, 30000); //启动一个定时，30s执行一次
		}
	}

	/**
	 * 这里最好是自定义一个事件，因为如果使用了Nacos的话，会冲突，这样的话需要修改SCG的源码，监听自定义的事件
	 * 我们就不这么做了，感兴趣的可以自行实现
	 */
	private void redisServicesWatch() {
		this.publisher.publishEvent( //30s发布一次事件，通知SCG重新拉取
				new HeartbeatEvent(this, redisWatchIndex.getAndIncrement()));
	}

	@Override
	public void stop() {
		if (this.running.compareAndSet(true, false) && this.watchFuture != null) {
			this.watchFuture.cancel(true);
		}
	}

	@Override
	public boolean isRunning() {
		return false;
	}
}
