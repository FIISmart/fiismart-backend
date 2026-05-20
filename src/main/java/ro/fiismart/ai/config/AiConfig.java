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
     * Multipart cap aligned with PdfAiService's 15 MB PDF limit so both
     * paths (parser-level and service-level) converge on the same client
     * error. File parts > 15 MB get rejected at the parser → 413
     * PDF_TOO_LARGE; the service-level check then acts as defense in
     * depth. The 16 MB request cap leaves headroom for the small form
     * fields (questionCount, language).
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(15));
        factory.setMaxRequestSize(DataSize.ofMegabytes(16));
        return factory.createMultipartConfig();
    }
}
