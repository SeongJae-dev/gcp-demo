package gcp.example.gcpdemo.service.gcp;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.MachineType;
import com.google.api.services.compute.model.Operation;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.stream.Collectors;

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

    @Autowired
    DiskConfigure diskConfigure;

    @Autowired
    InstanceConfigure instanceConfigure;

    public Compute createCompute(String applicationName) throws GeneralSecurityException, IOException {
        log.info("================== GCP Compute Build ==================");

        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredentials credential = GoogleCredentials.getApplicationDefault();
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credential);

        return new Compute.Builder(httpTransport, JSON_FACTORY, requestInitializer)
                .setApplicationName(applicationName)
                .build();

    }

    public boolean createComputeInstance(String instanceName, String applicationName) throws Exception, IOException, GeneralSecurityException {

        Compute compute = createCompute(applicationName);
        String e2MachineTypeSelfLink = diskConfigure.machineTypeList(compute).getItems()
                .stream()
                .filter(machineType -> (machineType.getName().contains("e2") && machineType.getName().contains("small")))
                .filter(machineType -> (machineType.getGuestCpus() <= 2 && machineType.getMemoryMb() <= 4096))
                .map(MachineType::getSelfLink)
                .collect(Collectors.joining());

//        MachineType machineType = new MachineType();
//        machineType.setGuestCpus(2);
//        machineType.setMemoryMb(4096);

//        Operation operation = createInstance(compute, instanceName, customMachineTypeSelfLink(machineType));
        Operation operation = instanceConfigure.createInstance(compute, instanceName, e2MachineTypeSelfLink);
        Operation.Error error = blockUntilComplete(compute, operation);

        boolean isCreated;
        if (error == null) {
            log.info("Create Instance Success!");
            isCreated = true;
        } else {
            log.info(error.toPrettyString());
            isCreated = false;
        }
        return  isCreated;

    }
}
