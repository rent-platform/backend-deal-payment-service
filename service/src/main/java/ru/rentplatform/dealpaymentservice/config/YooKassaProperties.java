package ru.rentplatform.dealpaymentservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "yookassa")
public class YooKassaProperties {

    private String shopId;

    private String secretKey;

    private String returnUrl;

    private boolean mockEnabled = false;
}
