package com.konkerlabs.platform.registry.test.business.services;

import com.konkerlabs.platform.registry.business.model.Device;
import com.konkerlabs.platform.registry.business.model.Event;
import com.konkerlabs.platform.registry.business.model.Tenant;
import com.konkerlabs.platform.registry.business.model.validation.CommonValidations;
import com.konkerlabs.platform.registry.business.repositories.DeviceRepository;
import com.konkerlabs.platform.registry.business.repositories.TenantRepository;
import com.konkerlabs.platform.registry.business.repositories.events.EventRepository;
import com.konkerlabs.platform.registry.business.services.api.DeviceEventService;
import com.konkerlabs.platform.registry.business.services.api.DeviceRegisterService;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;
import com.konkerlabs.platform.registry.config.PubServerConfig;
import com.konkerlabs.platform.registry.test.base.BusinessLayerTestSupport;
import com.konkerlabs.platform.registry.test.base.BusinessTestConfiguration;
import com.konkerlabs.platform.registry.test.base.MongoTestConfiguration;
import com.konkerlabs.platform.registry.test.base.RedisTestConfiguration;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import redis.clients.jedis.JedisPubSub;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static com.konkerlabs.platform.registry.test.base.matchers.ServiceResponseMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        MongoTestConfiguration.class,
        BusinessTestConfiguration.class,
        RedisTestConfiguration.class,
        PubServerConfig.class
})
@UsingDataSet(locations = {"/fixtures/tenants.json", "/fixtures/devices.json"})
public class DeviceEventServiceTest extends BusinessLayerTestSupport {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private DeviceEventService deviceEventService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    @Qualifier("mongoEvents")
    private EventRepository eventRepository;

    private String userDefinedDeviceGuid = "7d51c242-81db-11e6-a8c2-0746f010e945";
    private String guid = "71fc0d48-674a-4d62-b3e5-0216abca63af";
    private String apiKey = "84399b2e-d99e-11e5-86bc-34238775bac9";
    private String payload = "{\n" +
            "    \"ts\" : \"2016-03-03T18:15:00Z\",\n" +
            "    \"value\" : 31.0,\n" +
            "    \"command\" : {\n" +
            "      \"type\" : \"ButtonPressed\"\n" +
            "      },\n" +
            "    \"data\" : {\n" +
            "      \"channels\" : [\n" +
            "        { \"name\" : \"channel_0\" }\n" +
            "      ]\n" +
            "    },\n" +
            "    \"time\" : 123\n" +
            "  }";
    private String channel = "data";
    private String topic = MessageFormat.format("iot/{0}/{1}", apiKey, channel);
    private Event event;
    private Device device;
    private Tenant tenant;
    private Instant firstEventTimestamp;
    private Instant lastEventTimestamp;

    @Before
    public void setUp() throws Exception {
        firstEventTimestamp = Instant.ofEpochMilli(1474562670340L);
        lastEventTimestamp = Instant.ofEpochMilli(1474562674450L);

        tenant = tenantRepository.findByDomainName("konker");
        device = deviceRepository.findByTenantAndGuid(tenant.getId(), userDefinedDeviceGuid);
        event = Event.builder()
                .incoming(
                        Event.EventActor.builder()
                                .channel(channel)
                                .deviceGuid(device.getGuid()).build()
                ).payload(payload).build();
    }

    @Test
    public void shouldRaiseAnExceptionIfDeviceIsNull() throws Exception {
        ServiceResponse<Event> response = deviceEventService.logIncomingEvent(null, event);

        assertThat(response,hasErrorMessage(DeviceEventService.Validations.DEVICE_NULL.getCode()));
    }

    @Test
    public void shouldRaiseAnExceptionIfEventIsNull() throws Exception {
        ServiceResponse<Event> response = deviceEventService.logIncomingEvent(device, null);

        assertThat(response,hasErrorMessage(DeviceEventService.Validations.EVENT_NULL.getCode()));
    }

    @Test
    public void shouldRaiseAnExceptionIfPayloadIsNull() throws Exception {
        event.setPayload(null);

        ServiceResponse<Event> response = deviceEventService.logIncomingEvent(device, event);

        assertThat(response,hasErrorMessage(DeviceEventService.Validations.EVENT_PAYLOAD_NULL.getCode()));
    }

    @Test
    public void shouldRaiseAnExceptionIfPayloadIsEmpty() throws Exception {
        event.setPayload("");

        ServiceResponse<Event> response = deviceEventService.logIncomingEvent(device, event);

        assertThat(response,hasErrorMessage(DeviceEventService.Validations.EVENT_PAYLOAD_NULL.getCode()));
    }

    @Test
    public void shouldLogFirstDeviceEvent() throws Exception {
        deviceEventService.logIncomingEvent(device, event);

        Event last = eventRepository.findIncomingBy(tenant,device.getGuid(),channel,event.getTimestamp().minusSeconds(1l), null, false, 1).get(0);

        assertThat(last, notNullValue());

        long gap = Duration.between(last.getTimestamp(), Instant.now()).abs().getSeconds();

        assertThat(gap, not(greaterThan(60L)));
    }

    @Test
    public void shouldReturnAnErrorMessageIfTenantIsNullWhenFindingBy() throws Exception {

        ServiceResponse<List<Event>> serviceResponse = deviceEventService.findIncomingBy(
                null,
                device.getId(),channel,
                firstEventTimestamp,
                null,
                false,
                null
        );

        assertThat(serviceResponse, hasErrorMessage(CommonValidations.TENANT_NULL.getCode()));
    }

    @Test
    public void shouldReturnAnErrorMessageIfDeviceIdIsNullWhenFindingBy() throws Exception {

        ServiceResponse<List<Event>> serviceResponse = deviceEventService.findIncomingBy(
                tenant,
                null,channel,
                firstEventTimestamp,
                null,
                false,
                null
        );

        assertThat(serviceResponse, hasErrorMessage(DeviceRegisterService.Validations.DEVICE_GUID_NULL.getCode()));
    }

    @Test
    public void shouldReturnAnErrorMessageIfStartInstantIsNullAndLimitIsNullWhenFindingBy() throws Exception {

        ServiceResponse<List<Event>> serviceResponse = deviceEventService.findIncomingBy(
                tenant,
                device.getId(),channel,
                null,
                null,
                false,
                null
        );

        assertThat(serviceResponse, hasErrorMessage(DeviceEventService.Validations.LIMIT_NULL.getCode()));
    }

    @Test
    @UsingDataSet(locations = {"/fixtures/tenants.json","/fixtures/devices.json","/fixtures/deviceEvents.json"})
    public void shouldFindAllRequestEvents() throws Exception {
        ServiceResponse<List<Event>> serviceResponse = deviceEventService.findIncomingBy(
                tenant,
                device.getGuid(),"command",
                firstEventTimestamp,
                null,
                false,
                null
        );

        assertThat(serviceResponse.getResult(),notNullValue());
        assertThat(serviceResponse.getResult(),hasSize(3));

        assertThat(serviceResponse.getResult().get(0).getTimestamp().toEpochMilli(),
                equalTo(lastEventTimestamp.toEpochMilli()));
    }

}