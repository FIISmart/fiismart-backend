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
     * 20 MB cap on multipart uploads — accommodates the 15 MB PDF limit
     * enforced in PdfAiService plus multipart envelope overhead. Set
     * programmatically so the limit ships with the code rather than
     * depending on a (gitignored) application.properties entry.
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofMegabytes(20));
        factory.setMaxRequestSize(DataSize.ofMegabytes(20));
        return factory.createMultipartConfig();
    }
}
