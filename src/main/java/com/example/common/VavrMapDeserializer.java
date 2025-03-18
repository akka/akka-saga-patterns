package com.example.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.io.IOException;

public class VavrMapDeserializer extends JsonDeserializer<Map<?, ?>> {
  @Override
  public Map<?, ?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    // Read the JSON as a JsonNode
    JsonNode node = p.getCodec().readTree(p);
    // Convert the JsonNode to a java.util.Map
    java.util.Map<String, Object> javaMap = p.getCodec().treeToValue(node, java.util.Map.class);
    // Convert the java.util.Map to io.vavr.collection.Map
    return HashMap.ofAll(javaMap);
  }
}