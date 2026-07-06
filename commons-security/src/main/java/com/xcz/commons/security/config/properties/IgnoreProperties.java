package com.xcz.commons.security.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.ArrayList;
import java.util.List;

@Data
@RefreshScope
@ConfigurationProperties(prefix = "security.ignore")
public class IgnoreProperties {
    List<String> urls = new ArrayList<>();
}
