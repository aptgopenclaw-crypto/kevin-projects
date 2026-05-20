package com.taipei.iot.tender.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "tender.mail")
public class TenderMailProperties {

    /** 寄件者顯示名稱 */
    private String alias = "政府採購公告";

    /** 收件人清單 */
    private List<String> recipients = List.of();
}
