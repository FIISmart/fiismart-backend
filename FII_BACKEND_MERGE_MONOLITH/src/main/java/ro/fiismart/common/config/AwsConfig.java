package ro.fiismart.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsConfig {

    private final CognitoProperties cognitoProperties;

    @Value("${aws.region}")
    private String s3Region;

    public AwsConfig(CognitoProperties cognitoProperties) {
        this.cognitoProperties = cognitoProperties;
    }

    /**
     * Clientul Cognito folosește aws.cognito.region din CognitoProperties,
     * astfel încât regiunea Cognito și cea S3 pot fi configurate independent.
     */
    @Bean
    public CognitoIdentityProviderClient cognitoClient() {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(cognitoProperties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(s3Region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(s3Region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
