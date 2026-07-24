package com.xcz.commons.protobuf.model;

import com.xcz.commons.protobuf.annotation.ProtobufField;
import com.xcz.commons.protobuf.annotation.ProtobufMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@ProtobufMessage(comment = "带 Map 字段的测试消息")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleOrderWithMap {

    private Long orderId;

    @ProtobufField(comment = "扩展属性")
    private Map<String, String> attributes;
}
