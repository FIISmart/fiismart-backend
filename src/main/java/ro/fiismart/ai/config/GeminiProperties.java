package ro.fiismart.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "gemini.api")
public class GeminiProperties {
    private String baseUrl;
    private String model;
    private String key;
    private int timeoutSeconds;
}
