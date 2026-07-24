package com.xcz.commons.protobuf.runtime;

import com.xcz.commons.protobuf.model.SampleOrderWithMap;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtobufRuntimeTest {

    @Test
    void shouldSerializeAndDeserializeMapField() {
        ProtobufRuntime runtime = ProtobufRuntime.builder()
                .scanPackages("com.xcz.commons.protobuf.model")
                .build();

        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("region", "cn");
        attributes.put("channel", "app");

        SampleOrderWithMap source = SampleOrderWithMap.builder()
                .orderId(1001L)
                .attributes(attributes)
                .build();

        byte[] payload = runtime.serialize(source);
        SampleOrderWithMap decoded = runtime.deserialize(payload, SampleOrderWithMap.class);

        assertEquals(1001L, decoded.getOrderId());
        assertEquals("cn", decoded.getAttributes().get("region"));
        assertEquals("app", decoded.getAttributes().get("channel"));
    }

    @Test
    void shouldDeserializeByRoute() {
        ProtobufRuntime runtime = ProtobufRuntime.builder()
                .register(SampleOrderWithMap.class)
                .route("order-map", SampleOrderWithMap.class.getName())
                .build();

        SampleOrderWithMap source = SampleOrderWithMap.builder()
                .orderId(9L)
                .attributes(Map.of("k", "v"))
                .build();

        byte[] payload = runtime.serialize(source);
        Object decoded = runtime.deserializeByRoute("order-map", payload);

        assertTrue(decoded instanceof SampleOrderWithMap);
        assertEquals(9L, ((SampleOrderWithMap) decoded).getOrderId());
    }
}
