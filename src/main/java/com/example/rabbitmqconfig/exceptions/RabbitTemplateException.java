package com.example.rabbitmqconfig.exceptions;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;

public class RabbitTemplateException extends AmqpException {

    private final Message message;

    public RabbitTemplateException(String error, Message message) {
        super(error);
        this.message = message;
    }

    public Message getRabbitMessage() {
        return message;
    }
}
