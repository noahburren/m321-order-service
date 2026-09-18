package ch.tbz.m321orderservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.tbz.m321orderservice.event.OrderCreatedEvent;
import ch.tbz.m321orderservice.messaging.OrderEventPublisher;
import ch.tbz.m321orderservice.observability.CorrelationIdFilter;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.dynamic=false"
})
@AutoConfigureMockMvc
class SecurityIntegrationTests {

    @Autowired MockMvc mockMvc;
    @MockitoBean OrderEventPublisher eventPublisher;
    @Autowired JwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Test
    void orderWithoutJwtIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product\":\"Keyboard\",\"quantity\":2}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userMayCreateV1OrderAndSuppliedCorrelationIdReachesEvent() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .header(CorrelationIdFilter.HEADER_NAME, "demo-correlation-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product\":\"Keyboard\",\"quantity\":2}"))
                .andExpect(status().isAccepted())
                .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, "demo-correlation-123"))
                .andExpect(jsonPath("$.correlationId").value("demo-correlation-123"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        OrderCreatedEvent event = captor.getValue();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.orderId()).isNotNull();
        assertThat(event.correlationId()).isEqualTo("demo-correlation-123");
        assertThat(event.eventVersion()).isEqualTo(1);
    }

    @Test
    void missingCorrelationIdIsGenerated() throws Exception {
        String value = mockMvc.perform(post("/api/v1/orders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product\":\"Mouse\",\"quantity\":1}"))
                .andExpect(status().isAccepted()).andReturn()
                .getResponse().getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(value).isNotBlank();
        assertThat(UUID.fromString(value)).isNotNull();
    }

    @Test
    void v2WorksInParallelWithV1() throws Exception {
        mockMvc.perform(post("/api/v2/orders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"KEYBOARD-01\",\"amount\":2}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.orderId").isNotEmpty());
    }

    @Test
    void invalidOrderIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product\":\"\",\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userMayNotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminMayAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin")
                        .with(jwt().jwt(token -> token.subject("admin-123")
                                        .claim("preferred_username", "demo-admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("admin-123"));
    }

    @Test
    void keycloakRealmRolesAreConvertedToSpringAuthorities() {
        Jwt token = Jwt.withTokenValue("test-token").header("alg", "none")
                .subject("user-123").claim("preferred_username", "demo-user")
                .claim("realm_access", Map.of("roles", java.util.List.of("USER"))).build();
        var authentication = keycloakJwtAuthenticationConverter.convert(token);
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).extracting("authority").contains("ROLE_USER");
    }
}
