package gcp.example.gcpdemo.service.gcp;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class NetworkConfigure extends OperationErrorHandle{


    /**
     * Set the Network configuration values of the sample VM instance to be created.
     */
    private static final String NETWORK_INTERFACE_CONFIG = "ONE_TO_ONE_NAT";

    private static final String NETWORK_ACCESS_CONFIG = "External NAT";

    String applicationName = "gcpDemoApplication";

    @Autowired
    InstanceConfigure instanceConfigure;

    Compute compute;

    public NetworkConfigure() throws GeneralSecurityException, IOException {
        this.compute = new ComputeService().createCompute(applicationName);
    }

    public NetworkList networkList(Compute compute) throws IOException {
        log.info("================== GCP networkList List ==================");
        Compute.Networks.List request = compute.networks().list(PROJECT_ID);
        NetworkList response;
        do {
            response = request.execute();
            if (response.getItems() == null) {
                continue;
            }
            request.setPageToken(response.getNextPageToken());
        } while (response.getNextPageToken() != null);

        return response;
    }


    public NetworkInterface networkInterfaceConfig(Compute compute) throws IOException {
        log.info("================== GCP networkInterfaceConfig ==================");
        NetworkInterface networkInterface = new NetworkInterface();
        String networkSelfLink = networkList(compute).getItems()
                .stream()
                .map(Network::getSelfLink)
                .collect(Collectors.joining());

        networkInterface.setNetwork(networkSelfLink);

        List<AccessConfig> configs = new ArrayList<>();
        AccessConfig config = new AccessConfig();
        config.setType(NETWORK_INTERFACE_CONFIG);
        config.setName(NETWORK_ACCESS_CONFIG);
        configs.add(config);
        networkInterface.setAccessConfigs(configs);
//        networkInterface.setNetwork();

        return networkInterface;
    }

    public void autoIpToFixedIp() throws IOException {
        String instanceNatIp = instanceConfigure.getInstanceNatIp();
        log.info("instance -> {}", instanceNatIp);

    }
}
