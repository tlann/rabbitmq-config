package com.example.rabbitmqconfig.autoconfig;

import com.example.rabbitmqconfig.properties.RabbitProperties;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("rabbitadmin")
@EnableConfigurationProperties(RabbitProperties.class)
public class RabbitAdminConfig {
    @Autowired
    private RabbitProperties rabbitProperties;

    @Bean
    @ConditionalOnMissingBean
    public AmqpAdmin amqpAdmin(final ConnectionFactory connectionFactory) {
        AmqpAdmin admin = new RabbitAdmin(connectionFactory);
        if (rabbitProperties.getQueues() != null && rabbitProperties.getQueues().length > 0) {
            for (String queue : rabbitProperties.getQueues()) {
                admin.declareQueue(new Queue(queue));
            }
        }
        return admin;
    }
}