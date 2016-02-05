package com.konkerlabs.platform.registry.test.business.model;

import com.konkerlabs.platform.registry.business.model.EventRule;
import com.konkerlabs.platform.registry.business.model.Tenant;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.URI;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;

public class EventRuleTest {

    private EventRule subject;

    @Before
    public void setUp() throws Exception {

        Tenant tenant = Tenant.builder().name("Konker").build();

        subject = EventRule.builder()
            .tenant(tenant)
            .name("Rule name")
            .description("Description")
            .incoming(new EventRule.RuleActor(new URI("device://0000000000000004/")))
            .outgoing(new EventRule.RuleActor(new URI("device://0000000000000005/")))
            .transformations(Arrays.asList(new EventRule.RuleTransformation[]{
                    new EventRule.RuleTransformation("CONTENT_MATCH")
            }))
            .active(true)
            .build();
    }

    @Test
    public void shouldReturnAValidationMessageIfTenantIsNull() throws Exception {
        subject.setTenant(null);

        String expectedMessage = "Tenant cannot be null";

        assertThat(subject.applyValidations(), hasItem(expectedMessage));
    }
    @Test
    public void shouldReturnAValidationMessageIfNameIsNull() throws Exception {
        subject.setName(null);

        String expectedMessage = "Name cannot be null or empty";

        assertThat(subject.applyValidations(), hasItem(expectedMessage));
    }
    @Test
    public void shouldReturnAValidationMessageIfNameIsEmpty() throws Exception {
        subject.setName("");

        String expectedMessage = "Name cannot be null or empty";

        assertThat(subject.applyValidations(), hasItem(expectedMessage));
    }
    @Test
    public void shouldReturnAValidationMessageIfIncomingIsNull() throws Exception {
        subject.setIncoming(null);

        String expectedMessage = "Incoming actor cannot be null";

        assertThat(subject.applyValidations(), hasItem(expectedMessage));
    }
    @Test
    public void shouldReturnAValidationMessageIfIncomingURIIsNull() throws Exception {
        subject.getIncoming().setUri(null);

        String expectedMessage = "Incoming actor URI cannot be null";

        assertThat(subject.applyValidations(), hasItem(expectedMessage));
    }
    @Test
    public void shouldReturnAValidationMessageIfOutgoingIsNull() throws Exception {
        subject.setOutgoing(null);

        String expectedMessage = "Outgoing actor cannot be null";

        assertThat(subject.applyValidations(), hasItem(expectedMessage));
    }
    @Test
    public void shouldReturnAValidationMessageIfOutgoingURIIsNull() throws Exception {
        subject.getOutgoing().setUri(null);

        String expectedMessage = "Outgoing actor URI cannot be null";

        assertThat(subject.applyValidations(), hasItem(expectedMessage));
    }
    @Test
    public void shouldHaveNoValidationMessagesIfRecordIsValid() throws Exception {
        assertThat(subject.applyValidations(), nullValue());
    }
}
