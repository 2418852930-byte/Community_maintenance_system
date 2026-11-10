package com.community.repair.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 时间序列化配置。
 *
 * <p>实体与 VO 中的时间字段统一使用 {@link LocalDateTime}，
 * 若不做配置，Jackson 默认会输出成 {@code [2026,9,17,9,0,0]} 这样的数组，
 * 与接口文档约定的 {@code "2026-09-17 09:00:00"} 字符串格式不符。</p>
 *
 * <p>这里统一指定为 {@code yyyy-MM-dd HH:mm:ss}，前端可直接展示。</p>
 */
@Configuration
public class JacksonConfig {

    /** 日期时间格式：2026-09-17 09:00:00 */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 日期格式：2026-09-17 */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);

        SimpleModule module = new SimpleModule();
        // 序列化：LocalDateTime -> "yyyy-MM-dd HH:mm:ss"
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        // 反序列化：字符串 -> LocalDateTime（查询条件中的 startTime / endTime 需要）
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));

        return builder -> {
            // modulesToInstall：在 Jackson 默认模块（含 JavaTimeModule）之外追加自定义模块，
            // 不会覆盖 Spring Boot 已注册的内置模块
            builder.modulesToInstall(module);
            builder.simpleDateFormat(DATE_TIME_PATTERN);
            // 关闭「时间戳」输出，保证输出可读字符串
            builder.featuresToDisable(
                    com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}
