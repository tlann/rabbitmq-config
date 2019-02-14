package com.example.rabbitmqconfig.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("omns.rabbitmq")
public class RabbitProperties {
    private String[] queues;

    public String[] getQueues() {
        return queues;
    }

    public void setQueues(String[] queues) {
        this.queues = queues;
    }
}
