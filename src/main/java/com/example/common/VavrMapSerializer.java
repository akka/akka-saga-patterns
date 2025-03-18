package com.example.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.vavr.collection.Map;

import java.io.IOException;

public class VavrMapSerializer extends JsonSerializer<Map<?, ?>> {
  @Override
  public void serialize(Map<?, ?> vavrMap, JsonGenerator gen, SerializerProvider serializers) throws IOException {
    // Convert io.vavr.collection.Map to java.util.Map
    java.util.Map<?, ?> javaMap = vavrMap.toJavaMap();
    // Serialize the java.util.Map
    gen.writeObject(javaMap);
  }
}
