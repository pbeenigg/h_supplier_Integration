package com.heytrip.hotel.supplier.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * LocalDateTime参数绑定处理
 * 处理GET请求的日期时间字符串转换为LocalDateTime对象
 * 处理POST请求的JSON日期时间字符串转换为LocalDateTime对象
 * 支持多种日期时间格式
 *
 * @author Pax
 */
@ControllerAdvice
public class LocalDateTimeBinder {

    private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    );

    /**
     * 处理get请求
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(LocalDateTime.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                for (DateTimeFormatter formatter : FORMATTERS) {
                    try {
                        if (text.length() > 10) {
                            LocalDateTime parse = LocalDateTime.parse(text, formatter);
                            setValue(parse);
                            return;
                        }
                        LocalDate parse = LocalDate.parse(text, formatter);
                        setValue(parse.atStartOfDay());
                        return;
                    } catch (DateTimeParseException ignored) {
                    }
                }
                throw new IllegalArgumentException("Invalid date format: " + text);
            }
        });
    }

    /**
     * 处理post请求
     */
    @Bean
    public ObjectMapper objectMapper() {
        JavaTimeModule module = new JavaTimeModule();
        module.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }


    private static class CustomLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            String text = p.getText();
            for (DateTimeFormatter formatter : FORMATTERS) {
                try {
                    if (text.length() > 10) {
                        return LocalDateTime.parse(text, formatter);
                    }
                    return LocalDate.parse(text, formatter).atStartOfDay();
                } catch (DateTimeParseException ignored) {
                }
            }
            throw new IllegalArgumentException("Invalid date format: " + text);
        }
    }
}
