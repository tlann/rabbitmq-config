package com.example.rabbitmqconfig.factories;

import com.example.rabbitmqconfig.containers.ExampleMessageListenerContainer;
import org.springframework.amqp.rabbit.config.AbstractRabbitListenerContainerFactory;

public class ExampleMessageListenerContainerFactory extends AbstractRabbitListenerContainerFactory<ExampleMessageListenerContainer> {

//    DefaultTokenServices tokenServices;

//    public ExampleMessageListenerContainerFactory(DefaultTokenServices tokenServices) {
//        this.tokenServices = tokenServices;
//    }

    /**
     * Create an empty container instance.
     *
     * @return the new container instance.
     */
    @Override
    protected ExampleMessageListenerContainer createContainerInstance() {
        return new ExampleMessageListenerContainer();
    }
}
