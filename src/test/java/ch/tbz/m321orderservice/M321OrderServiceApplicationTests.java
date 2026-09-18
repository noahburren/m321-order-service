package ch.tbz.m321orderservice;

import static org.assertj.core.api.Assertions.assertThat;

import ch.tbz.m321orderservice.controller.OrderController;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.dynamic=false"
})
class M321OrderServiceApplicationTests {

    @Autowired OrderController orderController;
    @Autowired MessageConverter messageConverter;

    @Test
    void contextLoads() {
        assertThat(orderController).isNotNull();
        assertThat(messageConverter.getClass().getSimpleName()).isEqualTo("JacksonJsonMessageConverter");
    }

    @Test
    void contractsDescribeTheImplementation() throws Exception {
        String openApi = Files.readString(Path.of("openapi.yaml"));
        String asyncApi = Files.readString(Path.of("asyncapi.yaml"));
        assertThat(openApi).contains("/api/v1/orders:", "/api/v2/orders:", "X-Correlation-ID");
        assertThat(asyncApi).contains("orders.events", "order.created.v1", "order.processed.v1");
    }
}
