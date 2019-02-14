package com.example.rabbitmqconfig.factories;

import com.example.rabbitmqconfig.containers.ExampleMessageListenerContainer;
import org.springframework.amqp.rabbit.config.AbstractRabbitListenerContainerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoderJwkSupport;

@ComponentScan("com.example.utils.security.resource.autoconfig")
@Configuration
public class ExampleMessageListenerContainerFactory extends AbstractRabbitListenerContainerFactory<ExampleMessageListenerContainer> {

    @Autowired
    JwtDecoder jwtDecoder;

//    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
//    String issuerUri;
//

    public ExampleMessageListenerContainerFactory() {
    }

    /**
     * Create an empty container instance.
     *
     * @return the new container instance.
     */
    @Override
    protected ExampleMessageListenerContainer createContainerInstance() {
        String issuerUri = "http://172.18.36.70:8103/auth/realms/noms";
        jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuerUri);
        return new ExampleMessageListenerContainer(jwtDecoder);
    }

}
