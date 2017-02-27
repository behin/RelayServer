package edu.sharif.behin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.util.UriTemplate;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler extends AbstractWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    public static final String URI_TEMPLATE = "/WebSocket/{uniqueId}";
    private static final UriTemplate uriTemplate = new UriTemplate(URI_TEMPLATE);

    // map of uniqueId to session
    private Map<UUID, WebSocketSession> sessionMap = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        String uniqueId = uriTemplate.match(session.getUri().getPath()).get("uniqueId");
        logger.info(String.format("client with unique_id %s connected", uniqueId));
        sessionMap.put(UUID.fromString(uniqueId), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        String uniqueId = uriTemplate.match(session.getUri().getPath()).get("uniqueId");
        if(session == sessionMap.get(uniqueId)){
            logger.info(String.format("client with unique_id %s disconnected", uniqueId));
            sessionMap.remove(uniqueId);
        }else{
            logger.info(String.format("client with unique_id %s was disconnected but not removed", uniqueId));
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        super.handleBinaryMessage(session, message);
        UUID fromUUID = UUID.fromString(uriTemplate.match(session.getUri().getPath()).get("uniqueId"));
        if(message.getPayloadLength()<Long.BYTES*3){
            StringMessage stringMessage=new StringMessage();
            stringMessage.message = Constants.FAULT_MESSAGE;
            stringMessage.uuid = Constants.SERVER_UUID;
            try {
                String result = new ObjectMapper().writeValueAsString(stringMessage);
                session.sendMessage(new TextMessage(result));
            }catch (Exception e){
                logger.error("Cannot Parse String Message to Json",e);
            }
            return;
        }
        ByteBuffer payload = message.getPayload();
        ByteBuffer buffer = ByteBuffer.allocate(payload.remaining());
        long msb = payload.getLong();
        long lsb = payload.getLong();
        UUID toUUID = new UUID(msb,lsb);
        byte[] newMessagePayload = new byte[payload.remaining()];
        payload.get(newMessagePayload);
        logger.info("message relay from: "+fromUUID +
                " to "+ toUUID+ " size: "+ buffer.remaining());
        WebSocketSession toSession = sessionMap.get(toUUID);
        buffer.putLong(fromUUID.getMostSignificantBits());
        buffer.putLong(fromUUID.getLeastSignificantBits());
        buffer.put(newMessagePayload);
        if(toSession!=null){
            toSession.sendMessage(new BinaryMessage(buffer.array()));
        }else {
            StringMessage stringMessage=new StringMessage();
            stringMessage.message = Constants.NOT_FOUND_MESSAGE;
            stringMessage.uuid = Constants.SERVER_UUID;
            try {
                String result = new ObjectMapper().writeValueAsString(stringMessage);
                session.sendMessage(new TextMessage(result));
            }catch (Exception e){
                logger.error("Cannot Parse String Message to Json",e);
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        UUID fromUUID = UUID.fromString(uriTemplate.match(session.getUri().getPath()).get("uniqueId"));
        try {
            StringMessage stringMessage = new ObjectMapper().readValue(message.getPayload(),StringMessage.class);
            WebSocketSession toSession = sessionMap.get(stringMessage.uuid);
            if(toSession!=null){
                stringMessage.uuid = fromUUID;
                toSession.sendMessage(new TextMessage(new ObjectMapper().writeValueAsString(stringMessage)));
            } else {
                stringMessage.message = Constants.NOT_FOUND_MESSAGE;
                stringMessage.uuid = Constants.SERVER_UUID;
                String result = new ObjectMapper().writeValueAsString(stringMessage);
                session.sendMessage(new TextMessage(result));
            }
            stringMessage.uuid = fromUUID;
        }catch (Exception e){
            logger.error("Cannot Parse Message from :"+fromUUID,e);
        }
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        super.handlePongMessage(session, message);
    }

}
