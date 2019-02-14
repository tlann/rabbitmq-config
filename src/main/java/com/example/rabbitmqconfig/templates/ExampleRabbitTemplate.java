package com.example.rabbitmqconfig.templates;

import com.example.rabbitmqconfig.exceptions.RabbitTemplateException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.util.Objects;

public class ExampleRabbitTemplate extends RabbitTemplate {

    private final static Logger logger = LoggerFactory.getLogger(ExampleRabbitTemplate.class);

    @Autowired
    private ObjectMapper objectMapper;

    private void addAuthorizationHeader(Message message) {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken) {
                String token=((JwtAuthenticationToken)auth).getToken().getTokenValue();
                message.getMessageProperties().setHeader("Authorization", token);
            } else {
                throw new JwtException("Unable to get Jwt Token from SecurityContext");
            }
        } catch (Exception exc) {
            logger.error("Could not get Access Token ", exc);
            logger.debug(exc.getStackTrace().toString());
        }
    }

    private void setCorrelationId(Message message) {
        if (message.getMessageProperties().getCorrelationId() == null || Objects.equals(message.getMessageProperties().getCorrelationId(), "")) {
            message.getMessageProperties().setCorrelationId(java.util.UUID.randomUUID().toString());
        }
    }

    private void logOutgoing(String exchange, String routingKey, Message message, CorrelationData correlationData) {
//        LoggingUtil loggingUtil = new LoggingUtil();
//        loggingUtil.setEvent("Outgoing RabbitMQ " + exchange);
//        loggingUtil.setDest(routingKey);
//
//        logger.info(loggingUtil.toString());
        logger.debug(message.toString() + " CorreleationData: " + correlationData);
    }

    private void logReply(String exchange, String routingKey, Message message, CorrelationData correlationData) {
//        LoggingUtil loggingUtil = new LoggingUtil();
//        loggingUtil.setEvent("Reply RabbitMQ " + exchange);
//        loggingUtil.setSource(routingKey);
//
//        logger.info(loggingUtil.toString());
        logger.debug(message.toString() + " CorreleationData: "+ correlationData);
    }

    @Override
    public void send(Message message) throws AmqpException {
        send(getExchange(), getRoutingKey(), message);
    }

    @Override
    public void send(String routingKey, Message message) throws AmqpException {
        send(getExchange(), routingKey, message);
    }

    @Override
    public void send(String exchange, String routingKey, Message message) throws AmqpException {
        send(exchange, routingKey, message, null);
    }

    @Override
    public void send(String exchange, String routingKey, Message message, CorrelationData correlationData) throws AmqpException {
        super.send(exchange, routingKey, message, correlationData);
    }

    @Override
    protected void sendToRabbit(Channel channel, String exchange, String routingKey, boolean mandatory, Message message) throws IOException {
        addAuthorizationHeader(message);
        setCorrelationId(message);
        logOutgoing(exchange, routingKey, message, null);
        AMQP.BasicProperties convertedMessageProperties = getMessagePropertiesConverter().fromMessageProperties(message.getMessageProperties(), getEncoding());
        channel.basicPublish(exchange, routingKey, mandatory, convertedMessageProperties, message.getBody());
    }

    @Override
    public Message sendAndReceive(Message message) throws AmqpException {
        return sendAndReceive(message, null);
    }

    @Override
    public Message sendAndReceive(String routingKey, Message message) throws AmqpException {
        return sendAndReceive(getExchange(), routingKey, message, null);
    }

    @Override
    public Message sendAndReceive(Message message, CorrelationData correlationData) throws AmqpException {
        return sendAndReceive(getExchange(), getRoutingKey(), message, correlationData);
    }

    @Override
    public Message sendAndReceive(String exchange, String routingKey, Message message) throws AmqpException {
        return sendAndReceive(exchange, routingKey, message, null);
    }

    @Override
    public Message sendAndReceive(String routingKey, Message message, CorrelationData correlationData) throws AmqpException {
        return sendAndReceive(getExchange(), routingKey, message, correlationData);
    }

    @Override
    public Message sendAndReceive(String exchange, String routingKey, Message message, CorrelationData correlationData) throws AmqpException {
        addAuthorizationHeader(message);
        setCorrelationId(message);
        logOutgoing(exchange, routingKey, message, correlationData);
        Message replyMessage = super.sendAndReceive(exchange, routingKey, message, correlationData);
        logReply(exchange, routingKey, replyMessage, correlationData);
        return handleReply(replyMessage);
    }

    @Override
    protected Message convertSendAndReceiveRaw(String exchange, String routingKey, Object message, MessagePostProcessor messagePostProcessor, CorrelationData correlationData) {
        Message requestMessage = convertMessageIfNecessary(message);
        if (messagePostProcessor != null) {
            requestMessage = messagePostProcessor.postProcessMessage(requestMessage, correlationData);
        }
        addAuthorizationHeader(requestMessage);
        setCorrelationId(requestMessage);
        logOutgoing(exchange, routingKey, requestMessage, correlationData);
        Message replyMessage = doSendAndReceive(exchange, routingKey, requestMessage, correlationData);
        logReply(exchange, routingKey, replyMessage, correlationData);
        return handleReply(replyMessage);
    }

    private Message handleReply(Message replyMessage) {
        JsonNode replyNode = null;
        if (replyMessage != null) {
            try {
                replyNode = objectMapper.readTree(replyMessage.getBody());
            } catch (IOException exc) {
                logger.info("Object could not be converted ", exc);
                logger.debug(exc.getStackTrace().toString());
            }
        }
        if (replyNode == null) {
            throw new RabbitTemplateException("An error occurred while executing request.", replyMessage);
        } else {

            JsonNode errorNode = replyNode.get("errorMessage");
            if (errorNode == null || errorNode.isNull() || Objects.equals(errorNode.asText(), "")) {
                return replyMessage;
            } else {
                throw new RabbitTemplateException(errorNode.asText(), replyMessage);
            }
        }
    }

    @Override
    public Object convertSendAndReceive(Object message) throws AmqpException {
        return convertSendAndReceive(message, (CorrelationData) null);
    }

    @Override
    public Object convertSendAndReceive(String routingKey, Object message) throws AmqpException {
        return convertSendAndReceive(routingKey, message, (CorrelationData) null);
    }

    @Override
    public Object convertSendAndReceive(Object message, CorrelationData correlationData) throws AmqpException {
        return convertSendAndReceive(getExchange(), getRoutingKey(), message, null, correlationData);
    }

    @Override
    public Object convertSendAndReceive(String exchange, String routingKey, Object message) throws AmqpException {
        return convertSendAndReceive(exchange, routingKey, message, (CorrelationData) null);
    }

    @Override
    public Object convertSendAndReceive(Object message, MessagePostProcessor messagePostProcessor) throws AmqpException {
        return convertSendAndReceive(message, messagePostProcessor, null);
    }

    @Override
    public Object convertSendAndReceive(String routingKey, Object message, CorrelationData correlationData) throws AmqpException {
        return convertSendAndReceive(getExchange(), routingKey, message, null, correlationData);
    }

    @Override
    public Object convertSendAndReceive(String routingKey, Object message, MessagePostProcessor messagePostProcessor) throws AmqpException {
        return convertSendAndReceive(getExchange(), routingKey, message, messagePostProcessor, null);
    }

    @Override
    public Object convertSendAndReceive(String exchange, String routingKey, Object message, CorrelationData correlationData) throws AmqpException {
        return convertSendAndReceive(exchange, routingKey, message, null, correlationData);
    }

    @Override
    public Object convertSendAndReceive(Object message, MessagePostProcessor messagePostProcessor, CorrelationData correlationData) throws AmqpException {
        return convertSendAndReceive(getExchange(), getRoutingKey(), message, messagePostProcessor, correlationData);
    }

    @Override
    public Object convertSendAndReceive(String exchange, String routingKey, Object message, MessagePostProcessor messagePostProcessor) throws AmqpException {
        return convertSendAndReceive(exchange, routingKey, message, messagePostProcessor, null);
    }

    @Override
    public Object convertSendAndReceive(String routingKey, Object message, MessagePostProcessor messagePostProcessor, CorrelationData correlationData) throws AmqpException {
        return convertSendAndReceive(getExchange(), routingKey, message, messagePostProcessor, correlationData);
    }

    @Override
    public Object convertSendAndReceive(String exchange, String routingKey, Object message, MessagePostProcessor messagePostProcessor, CorrelationData correlationData) throws AmqpException {
        return convertSendAndReceiveRaw(exchange, routingKey, message, messagePostProcessor, correlationData);
    }

    @Override
    public <T> T convertSendAndReceiveAsType(Object message, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return convertSendAndReceiveAsType(message, (CorrelationData) null, responseType);
    }

    @Override
    public <T> T convertSendAndReceiveAsType(String routingKey, Object message, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return convertSendAndReceiveAsType(routingKey, message, (CorrelationData) null, responseType);
    }

    @Override
    public <T> T convertSendAndReceiveAsType(Object message, CorrelationData correlationData, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return convertSendAndReceiveAsType(getExchange(), getRoutingKey(), message, null, correlationData, responseType);
    }

    @Override
    public <T> T convertSendAndReceiveAsType(String exchange, String routingKey, Object message, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return convertSendAndReceiveAsType(exchange, routingKey, message, (CorrelationData) null, responseType);
    }

    @Override
    public <T> T convertSendAndReceiveAsType(Object message, MessagePostProcessor messagePostProcessor, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return convertSendAndReceiveAsType(message, messagePostProcessor, null, responseType);
    }

    @Override
    public <T> T convertSendAndReceiveAsType(String routingKey, Object message, CorrelationData correlationData, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return convertSendAndReceiveAsType(getExchange(), routingKey, message, null, correlationData, responseType);
    }

    @Override
    public <T> T convertSendAndReceiveAsType(String routingKey, Object message, MessagePostProcessor messagePostProcessor, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return convertSendAndReceiveAsType(routingKey, message, messagePostProcessor, null, responseType);
    }

    @Override
    public <T> T convertSendAndReceiveAsType(String exchange, String routingKey, Object message, CorrelationData correlationData, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return convertSendAndReceiveAsType(exchange, routingKey, message, null, correlationData, responseType);
    }

    @Override
    public <T> T convertSendAndReceiveAsType(Object message, MessagePostProcessor messagePostProcessor, CorrelationData correlationData, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return convertSendAndReceiveAsType(getExchange(), getRoutingKey(), message, messagePostProcessor, correlationData, responseType);
    }

    @Override
    public <T> T convertSendAndReceiveAsType(String exchange, String routingKey, Object message, MessagePostProcessor messagePostProcessor, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return convertSendAndReceiveAsType(exchange, routingKey, message, messagePostProcessor, null, responseType);
    }

    @Override
    public <T> T convertSendAndReceiveAsType(String routingKey, Object message, MessagePostProcessor messagePostProcessor, CorrelationData correlationData, ParameterizedTypeReference<T> responseType) throws AmqpException {
        return convertSendAndReceiveAsType(getExchange(), routingKey, message, messagePostProcessor, correlationData, responseType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convertSendAndReceiveAsType(String exchange, String routingKey, Object message, MessagePostProcessor messagePostProcessor, CorrelationData correlationData, ParameterizedTypeReference<T> responseType) throws AmqpException {
        Message replyMessage = convertSendAndReceiveRaw(exchange, routingKey, message, messagePostProcessor, correlationData);

        if (replyMessage == null) {
            return null;
        }
        return (T) ((SmartMessageConverter) getMessageConverter()).fromMessage(replyMessage, responseType);
    }

    @Override
    public void convertAndSend(Object object) throws AmqpException {
        convertAndSend(getExchange(), getRoutingKey(), object, (CorrelationData) null);
    }

    @Override
    public void convertAndSend(String routingKey, Object object) throws AmqpException {
        convertAndSend(getExchange(), routingKey, object, (CorrelationData) null);
    }

    @Override
    public void convertAndSend(String exchange, String routingKey, Object object) throws AmqpException {
        convertAndSend(exchange, routingKey, object, (CorrelationData) null);
    }

    @Override
    public void convertAndSend(String routingKey, Object object, CorrelationData correlationData) throws AmqpException {
        convertAndSend(getExchange(), routingKey, object, correlationData);
    }

    @Override
    public void convertAndSend(String exchange, String routingKey, Object object, CorrelationData correlationData) throws AmqpException {
        send(exchange, routingKey, convertMessageIfNecessary(object), correlationData);
    }

    @Override
    public void convertAndSend(Object message, MessagePostProcessor messagePostProcessor) throws AmqpException {
        convertAndSend(getExchange(), getRoutingKey(), message, messagePostProcessor);
    }

    @Override
    public void convertAndSend(String routingKey, Object message, MessagePostProcessor messagePostProcessor) throws AmqpException {
        convertAndSend(getExchange(), routingKey, message, messagePostProcessor, null);
    }

    @Override
    public void convertAndSend(Object message, MessagePostProcessor messagePostProcessor, CorrelationData correlationData) throws AmqpException {
        convertAndSend(getExchange(), getRoutingKey(), message, messagePostProcessor, correlationData);
    }

    @Override
    public void convertAndSend(String exchange, String routingKey, Object message, MessagePostProcessor messagePostProcessor) throws AmqpException {
        convertAndSend(exchange, routingKey, message, messagePostProcessor, null);
    }

    @Override
    public void convertAndSend(String routingKey, Object message, MessagePostProcessor messagePostProcessor, CorrelationData correlationData) throws AmqpException {
        convertAndSend(getExchange(), routingKey, message, messagePostProcessor, correlationData);
    }

    @Override
    public void convertAndSend(String exchange, String routingKey, Object message, MessagePostProcessor messagePostProcessor, CorrelationData correlationData) throws AmqpException {
        Message messageToSend = convertMessageIfNecessary(message);
        messageToSend = messagePostProcessor.postProcessMessage(messageToSend);
        send(exchange, routingKey, messageToSend, correlationData);
    }

    @Override
    public void correlationConvertAndSend(Object object, CorrelationData correlationData) throws AmqpException {
        convertAndSend(getExchange(), getRoutingKey(), object, correlationData);
    }

}
