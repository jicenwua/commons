package com.xcz.commons.protobuf.model;

import com.xcz.commons.protobuf.annotation.ProtobufField;
import com.xcz.commons.protobuf.annotation.ProtobufMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@ProtobufMessage(comment = "测试订单消息")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SampleOrder {

    @ProtobufField(comment = "订单主键")
    private Long orderId;
    private Long userID;
    private BigDecimal amount;
}
