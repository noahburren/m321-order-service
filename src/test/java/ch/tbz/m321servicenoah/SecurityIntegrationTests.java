package ch.tbz.m321servicenoah;

import java.time.Instant;
import java.util.Map;

import ch.tbz.m321servicenoah.messaging.OrderProducer;
import ch.tbz.m321servicenoah.model.OrderMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.dynamic=false",
        "messaging.auto-producer.enabled=false"
})
@AutoConfigureMockMvc
class SecurityIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderProducer orderProducer;

    @Autowired
    private JwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Test
    void orderWithoutJwtIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"product":"Keyboard","quantity":2}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rootRedirectsToPublicSwaggerUi() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/swagger-ui.html"));
    }

    @Test
    void userMayCreateOrder() throws Exception {
        when(orderProducer.createOrder(anyString(), anyInt()))
                .thenReturn(new OrderMessage(101L, "Keyboard", 2, Instant.parse("2026-09-18T10:00:00Z")));

        mockMvc.perform(post("/api/orders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"product":"Keyboard","quantity":2}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.orderId").value(101));
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
                        .with(jwt()
                                .jwt(token -> token
                                        .subject("admin-123")
                                        .claim("preferred_username", "demo-admin"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("admin-123"))
                .andExpect(jsonPath("$.username").value("demo-admin"));
    }

    @Test
    void keycloakRealmRolesAreConvertedToSpringAuthorities() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("user-123")
                .claim("preferred_username", "demo-user")
                .claim("realm_access", Map.of("roles", java.util.List.of("USER")))
                .build();

        var authentication = keycloakJwtAuthenticationConverter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("demo-user");
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_USER");
    }
}
