package com.xcz.member.core.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@AutoConfiguration
public class DateFormatConfig {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    /**
     * 支持多种格式的反序列化器：优先尝试 ISO-8601 格式，失败后尝试自定义格式
     */
    static class FlexibleLocalDateTimeDeserializer extends LocalDateTimeDeserializer {
        private static final long serialVersionUID = 1L;

        protected FlexibleLocalDateTimeDeserializer() {
            super(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        @Override
        public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String value = parser.getValueAsString();
            if (value == null || value.isEmpty()) {
                return null;
            }

            try {
                // 优先尝试 ISO-8601 格式 (2026-06-19T00:00:00)
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e) {
                try {
                    // 如果失败，尝试自定义格式 (2026-06-19 00:00:00)
                    return LocalDateTime.parse(value, FORMATTER);
                } catch (DateTimeParseException ex) {
                    log.warn("无法解析时间字符串: {}, 尝试的格式: ISO-8601 和 yyyy-MM-dd HH:mm:ss", value);
                    throw ex;
                }
            }
        }
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        log.info("统一设置时间格式，支持 ISO-8601 和 yyyy-MM-dd HH:mm:ss 格式");
        return builder -> {
            // 1. 序列化：LocalDateTime -> String (返回给前端)
            builder.serializerByType(LocalDateTime.class,
                    new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));

            // 2. 反序列化：String -> LocalDateTime (接收前端参数，支持多种格式)
            builder.deserializerByType(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());
        };
    }
}
