package gcp.example.gcpdemo.service.gcp;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Component
@Slf4j
public class ComputeService extends OperationErrorHandle{

    //  private static final String PROJECT_ID = "YOUR_PROJECT_ID";
    public static final String PROJECT_ID = "flash-precept-306501";

    /**
     * Set Compute Engine zone.
     */
    public static final String ZONE_NAME = "us-central1-a";

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport httpTransport;

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public Compute createCompute(String applicationName) throws GeneralSecurityException, IOException {
        log.info("================== GCP Compute Build ==================");

        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredentials credential = GoogleCredentials.getApplicationDefault();
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credential);

        return new Compute.Builder(httpTransport, JSON_FACTORY, requestInitializer)
                .setApplicationName(applicationName)
                .build();

    }
}
