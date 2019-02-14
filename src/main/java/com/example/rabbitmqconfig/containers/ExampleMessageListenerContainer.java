package com.example.rabbitmqconfig.containers;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

/**
 * Custom Message Listener Container which is used to intercept incoming messages and
 * validate that there is a user token associated with the request. If there is no token,
 * and there is a reply to address, the error message is returned to the requestor, and the
 * message is dropped. If there is no reply to address, the message is simply dropped.
 */
public class ExampleMessageListenerContainer extends SimpleMessageListenerContainer {
    private final static String errorMessage = "No valid credentials found in request: {}";
    private final static Logger logger = LoggerFactory.getLogger(ExampleMessageListenerContainer.class);


    @Autowired
    @Qualifier("jwtDecoderByIssuerUri")
    JwtDecoder jwtDecoder;

    @Autowired
    AuthenticationManager authenticationManager;

    /**
     * Constructor
     *
     * @param tokenServices The instance of DefaultTokenServices used to decrypt the access token.
     */
//    public ExampleMessageListenerContainer(DefaultTokenServices tokenServices) {
//        this.tokenServices = tokenServices;
//    }

    /**
     * This method checks to see if there is a valid authorization
     *
     * @param channel   The AMQP channel on which the message was published.
     * @param messageIn The incoming message.
     * @throws Exception Throws an exception when there are no valid credentials in the message.
     */
    @Override
    protected void executeListener(Channel channel, Message messageIn) throws Exception {
//        LoggingUtil loggingUtil = new LoggingUtil();
//        loggingUtil.setSource(getQueueNames().toString());
//        loggingUtil.setEvent("Message Listener");
//        loggingUtil.setDest(messageIn.getMessageProperties().getReplyTo());
//        loggingUtil.setMessage(messageIn.toString());
//        logger.info(loggingUtil.toString());

        if (messageIn.getMessageProperties().getHeaders().keySet().stream().anyMatch(t -> Objects.equals(t.toLowerCase(), "authorization"))) {
            String accessKey = messageIn.getMessageProperties()
                    .getHeaders()
                    .keySet()
                    .stream()
                    .filter(t -> Objects.equals(t.toLowerCase(), "authorization"))
                    .findFirst()
                    .get();

            JwtAuthenticationToken auth = null;
            String accessToken = null;
            try {
                if(accessKey != null) {
                    accessToken = messageIn.getMessageProperties().getHeaders().get(accessKey).toString();
                    auth = new JwtAuthenticationToken(jwtDecoder.decode(accessToken));
                }
            } catch (AuthenticationException | JwtException exc) {
                logger.error("Could not load Authentication ", exc);
                logger.debug(exc.getStackTrace().toString());
            }
            // If the token is expired, there will be no auth.
            if (auth != null && auth.isAuthenticated()) {
                ((HashMap) auth.getDetails()).put("accessToken", accessToken);
                SecurityContextHolder.getContext().setAuthentication(auth);
                super.executeListener(channel, messageIn);
                return;
            }
        }
        rejectMessage(channel, messageIn);
    }

    private void rejectMessage(Channel channel, Message messageIn) throws AmqpRejectAndDontRequeueException, IOException {
//        LoggingUtil loggingUtil = new LoggingUtil();
//        loggingUtil.setMessage(errorMessage + getQueueNames());
//        logger.info(loggingUtil.toString());
        String localMessage = errorMessage.replace("{}", String.join(", ", getQueueNames()));
        if (messageIn.getMessageProperties().getReplyTo() != null) {
            channel.basicPublish("",
                    messageIn.getMessageProperties().getReplyTo(),
                    new AMQP.BasicProperties.Builder()
                            .contentType("application/json")
                            .correlationId(messageIn.getMessageProperties().getCorrelationId())
                            .build(),
                    "{\"errorMessage\":\"".concat(localMessage).concat("\"}").getBytes());
        }
        throw new AmqpRejectAndDontRequeueException(localMessage);
    }
}
