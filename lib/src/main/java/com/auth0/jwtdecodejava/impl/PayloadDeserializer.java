package com.auth0.jwtdecodejava.impl;

import com.auth0.jwtdecodejava.exceptions.JWTException;
import com.auth0.jwtdecodejava.interfaces.Payload;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class PayloadDeserializer extends StdDeserializer<Payload> {

    PayloadDeserializer() {
        this(null);
    }

    private PayloadDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Payload deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Map<String, JsonNode> tree = p.getCodec().readValue(p, new TypeReference<Map<String, JsonNode>>() {
        });
        if (tree == null) {
            throw new JWTException("Null map");
        }

        String issuer = getString(tree, Claims.ISSUER);
        String subject = getString(tree, Claims.SUBJECT);
        String[] audience = getStringOrArray(tree, Claims.AUDIENCE);
        Date expiresAt = getDate(tree, Claims.EXPIRES_AT);
        Date notBefore = getDate(tree, Claims.NOT_BEFORE);
        Date issuedAt = getDate(tree, Claims.ISSUED_AT);
        String jwtId = getString(tree, Claims.JWT_ID);

        return new PayloadImpl(issuer, subject, audience, expiresAt, notBefore, issuedAt, jwtId, tree);
    }

    private String[] getStringOrArray(Map<String, JsonNode> tree, String claimName) {
        JsonNode node = tree.get(claimName);
        if (node == null || node.isNull() || !(node.isArray() || node.isTextual())) {
            return null;
        }
        if (node.isTextual() && !node.asText().isEmpty()) {
            return new String[]{node.asText()};
        }

        ObjectMapper mapper = new ObjectMapper();
        String[] arr = new String[node.size()];
        for (int i = 0; i < node.size(); i++) {
            try {
                arr[i] = mapper.treeToValue(node.get(i), String.class);
            } catch (JsonProcessingException e) {
                throw new JWTException("Couldn't map the Claim's array contents to String", e);
            }
        }
        return arr;
    }

    private Date getDate(Map<String, JsonNode> tree, String claimName) {
        JsonNode node = tree.get(claimName);
        if (node == null || node.isNull() || !node.canConvertToLong()) {
            return null;
        }
        final long ms = node.asLong() * 1000;
        return new Date(ms);
    }

    private String getString(Map<String, JsonNode> tree, String claimName) {
        JsonNode node = tree.get(claimName);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText(null);
    }
}
