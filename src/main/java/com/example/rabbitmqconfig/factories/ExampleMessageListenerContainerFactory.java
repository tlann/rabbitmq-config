package com.example.rabbitmqconfig.factories;

import com.example.rabbitmqconfig.containers.ExampleMessageListenerContainer;
import org.springframework.amqp.rabbit.config.AbstractRabbitListenerContainerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;

@ComponentScan("com.example.utils.security.resource.autoconfig")
@Configuration
public class ExampleMessageListenerContainerFactory extends AbstractRabbitListenerContainerFactory<ExampleMessageListenerContainer> {

    @Autowired
    JwtDecoder jwtDecoder;

    public ExampleMessageListenerContainerFactory() {
    }

    /**
     * Create an empty container instance.
     *
     * @return the new container instance.
     */
    @Override
    protected ExampleMessageListenerContainer createContainerInstance() {
//        String issuerUri = "http://172.18.36.70:8103/auth/realms/omns";
//        jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuerUri);
        return new ExampleMessageListenerContainer(jwtDecoder);
    }

}
