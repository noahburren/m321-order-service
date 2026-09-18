package ch.tbz.m321orderservice.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String ORDERS_EVENTS_EXCHANGE = "orders.events";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created.v1";
    public static final String ORDER_PROCESSED_ROUTING_KEY = "order.processed.v1";
    public static final String ORDER_PROCESSING_QUEUE = "orders.processing";
    public static final String ORDER_PROCESSED_QUEUE = "orders.order-service.processed";

    @Bean
    TopicExchange ordersEventsExchange() {
        return new TopicExchange(ORDERS_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    Queue orderProcessingQueue() {
        return QueueBuilder.durable(ORDER_PROCESSING_QUEUE).build();
    }

    @Bean
    Queue orderProcessedQueue() {
        return QueueBuilder.durable(ORDER_PROCESSED_QUEUE).build();
    }

    @Bean
    Binding orderCreatedBinding(TopicExchange ordersEventsExchange) {
        return BindingBuilder.bind(orderProcessingQueue())
                .to(ordersEventsExchange)
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    Binding orderProcessedBinding(TopicExchange ordersEventsExchange) {
        return BindingBuilder.bind(orderProcessedQueue())
                .to(ordersEventsExchange)
                .with(ORDER_PROCESSED_ROUTING_KEY);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
