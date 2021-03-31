package org.springframework.cloud.gateway.sample.route;

import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author li.hongjian
 * @email lhj502819@163.com
 * @Date 2021/3/31
 */
@Service
public class RedisRouteDefinitionWatch implements ApplicationEventPublisherAware, SmartLifecycle {

	private final TaskScheduler taskScheduler = getTaskScheduler();

	private final AtomicLong redisWatchIndex = new AtomicLong(0);

	private final AtomicBoolean running = new AtomicBoolean(false);

	private ApplicationEventPublisher publisher;

	private ScheduledFuture<?> watchFuture;

	private static ThreadPoolTaskScheduler getTaskScheduler() {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setBeanName("Nacso-Watch-Task-Scheduler");
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

	private void redisServicesWatch() {
		// nacos doesn't support watch now , publish an event every 30 seconds.
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
