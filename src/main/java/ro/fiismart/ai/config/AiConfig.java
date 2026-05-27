package ro.fiismart.ai.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class AiConfig {

    @Bean
    public RestClient geminiRestClient(GeminiProperties props) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(props.getTimeoutSeconds()));

        return RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 50 MB multipart cap — accommodates the lecture-file upload flow
     * (PDF/DOC/ZIP/...) at /api/v1/files/lecture which caps at 50 MB.
     * The AI flow's own 15 MB check still rejects oversize PDFs at the
     * service layer with a clear 400 message, so the looser parser cap
     * does not weaken AI input validation.
     */

}
