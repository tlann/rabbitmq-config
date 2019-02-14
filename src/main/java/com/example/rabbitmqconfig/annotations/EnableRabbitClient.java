package com.example.rabbitmqconfig.annotations;

import com.example.rabbitmqconfig.autoconfig.RabbitConfig;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AutoConfigureAfter(RabbitAutoConfiguration.class)
@Import(RabbitConfig.class)
public @interface EnableRabbitClient {
}
