package ro.fiismart.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CognitoPropertiesTest {

    @Test
    void getJwksUri_returnsFormattedUrl() {
        CognitoProperties props = new CognitoProperties();
        props.setRegion("eu-central-1");
        props.setUserPoolId("eu-central-1_ABC123");

        String jwks = props.getJwksUri();

        assertThat(jwks).isEqualTo(
            "https://cognito-idp.eu-central-1.amazonaws.com/eu-central-1_ABC123/.well-known/jwks.json"
        );
    }

    @Test
    void getIssuerUri_returnsFormattedUrl() {
        CognitoProperties props = new CognitoProperties();
        props.setRegion("us-east-1");
        props.setUserPoolId("us-east-1_XYZ");

        String issuer = props.getIssuerUri();

        assertThat(issuer).isEqualTo(
            "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_XYZ"
        );
    }

    @Test
    void defaultValues_areBlankStrings() {
        CognitoProperties props = new CognitoProperties();
        assertThat(props.getRegion()).isEqualTo("");
        assertThat(props.getUserPoolId()).isEqualTo("");
        assertThat(props.getClientId()).isEqualTo("");
        assertThat(props.getClientSecret()).isEqualTo("");
        assertThat(props.getRedirectUri()).isEqualTo("http://localhost:3000/auth/callback");
    }

    @Test
    void setters_andGetters_work() {
        CognitoProperties props = new CognitoProperties();
        props.setClientId("client123");
        props.setClientSecret("secret456");
        props.setHostedUiDomain("domain.auth.us-east-1.amazoncognito.com");
        props.setRedirectUri("http://example.com/callback");

        assertThat(props.getClientId()).isEqualTo("client123");
        assertThat(props.getClientSecret()).isEqualTo("secret456");
        assertThat(props.getHostedUiDomain()).isEqualTo("domain.auth.us-east-1.amazoncognito.com");
        assertThat(props.getRedirectUri()).isEqualTo("http://example.com/callback");
    }
}
