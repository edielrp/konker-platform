package com.konkerlabs.platform.registry.business.services;

import com.konkerlabs.platform.registry.business.model.Device;
import com.konkerlabs.platform.registry.business.model.Event;
import com.konkerlabs.platform.registry.business.services.api.DeviceEventService;
import com.konkerlabs.platform.registry.business.services.api.DeviceRegisterService;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.time.Instant;
import java.util.List;

@Service
public class JedisTaskService {
	
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	
	@Autowired
	private DeviceEventService deviceEventService;
	
	@Autowired
	private DeviceRegisterService deviceRegisterService;
	
	protected ServiceResponse<List<Event>> response;

	public JedisTaskService() {
		
	}

	public List<Event> subscribeToChannel(String channel, Instant startTimestamp, boolean asc, Integer limit) {
		Jedis jedis = (Jedis) redisTemplate.getConnectionFactory().getConnection().getNativeConnection();
		JedisPubSub jedisPubSub = new JedisPubSub() {
			@Override
			public void onMessage(String channel, String message) {
				Device device = deviceRegisterService.findByApiKey(channel.substring(0, channel.indexOf(".")));
				response = deviceEventService.findOutgoingBy(device.getTenant(), device.getGuid(),
						channel.substring(channel.indexOf(".") + 1), startTimestamp, null, asc, limit);
				this.unsubscribe(channel);
			}
		};
		
		jedis.subscribe(jedisPubSub, channel);
		
		return response.getResult();
	}
	
}
