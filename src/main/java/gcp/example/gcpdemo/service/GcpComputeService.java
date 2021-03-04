package gcp.example.gcpdemo.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GcpComputeService {


    //  private static final String PROJECT_ID = "YOUR_PROJECT_ID";
    private static final String PROJECT_ID = "flash-precept-306501";

    /**
     * Set Compute Engine zone.
     */
    private static final String ZONE_NAME = "us-central1-a";

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport httpTransport;

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * Set the Network configuration values of the sample VM instance to be created.
     */
    private static final String NETWORK_INTERFACE_CONFIG = "ONE_TO_ONE_NAT";

    private static final String NETWORK_ACCESS_CONFIG = "External NAT";

    /**
     * Set the path of the OS image for the sample VM instance to be created.
     */
    private static final String SOURCE_IMAGE_PREFIX =
            "https://www.googleapis.com/compute/v1/projects/";

    private static final String SOURCE_IMAGE_PATH =
            "ubuntu-os-cloud/global/images/ubuntu-2004-focal-v20200529";

    private static final long OPERATION_TIMEOUT_MILLIS = 60 * 1000;

    public static void main(String[] args) throws Exception {
        GcpComputeService gcpComputeService = new GcpComputeService();
        gcpComputeService.createComputeInstance();
    }


    public void createComputeInstance() throws Exception{

        String applicationName = "gcpDemoApplication";
        String instanceName = "gcp-demo-instance00";

        Compute compute = createCompute(applicationName);

        // project에 존재하는 instance list 가져옴
        boolean existInstance = listInstance(compute);
        log.info("existInstance -> {}",existInstance);

        // 해당 zone에 존재하는 머신 리스트 정보 가져옴
//        MachineTypeList mtList = machineTypeList(compute);

        String e2MachineTypeSelfLink = machineTypeList(compute).getItems()
                .stream()
                .filter(machineType -> (machineType.getName().contains("e2") && machineType.getName().contains("small")))
                .filter(machineType -> (machineType.getGuestCpus() <= 2 && machineType.getMemoryMb() <= 4096))
                .map(MachineType::getSelfLink)
                .collect(Collectors.joining());

        log.info("e2MachineTypeSelfLink -> {}", e2MachineTypeSelfLink);

        Operation operation = createInstance(compute, instanceName, e2MachineTypeSelfLink);


//        return operation;
    }

    public Operation createInstance(Compute compute, String instanceName, String machineTypeSelfLink )throws IOException {
        log.info("================== Starting New Instance ==================");

        //instance 설정
        Instance instance = new Instance();
        instance.setName(instanceName);
        instance.setMachineType(machineTypeSelfLink);

        //NetWork interface 설정
        instance.setNetworkInterfaces(Collections.singletonList(networkInterfaceConfig(compute)));
        //Disk 설정
        instance.setDisks(Collections.singletonList(diskConfig(compute, instanceName)));

        return new Operation();
    }

    public Compute createCompute (String applicationName) throws GeneralSecurityException, IOException {
        log.info("================== GCP Compute Build ==================");

        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredentials credential = GoogleCredentials.getApplicationDefault();
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credential);

        return new Compute.Builder(httpTransport, JSON_FACTORY, requestInitializer)
                .setApplicationName(applicationName)
                .build();

    }

    public boolean listInstance(Compute compute) throws  IOException{
        log.info("================== GCP Compute Instance List ==================");

        Compute.Instances.List instances = compute.instances().list(PROJECT_ID, ZONE_NAME);
        InstanceList instanceList = instances.execute();
        log.info("instanceList -> {} ",instanceList );
//        boolean existInstance = false;
        List<Instance> instancesItem = Optional
                .ofNullable(instanceList.getItems())
                .orElseGet(ArrayList::new);

        log.info("instancesItem -> {}", instancesItem);

        return instancesItem.size() > 1;
    }

    public  MachineTypeList machineTypeList(Compute compute) throws IOException {
        log.info("================== GCP MachineType List ==================");
        Compute.MachineTypes.List request = compute.machineTypes().list(PROJECT_ID, ZONE_NAME);
        MachineTypeList response;

        do {
            response = request.execute();
            if (response.getItems() == null) {
                continue;
            }
            request.setPageToken(response.getNextPageToken());
        } while (response.getNextPageToken() != null);


        return response;
    }

    public  NetworkList networkList(Compute compute) throws IOException {
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

    public AttachedDisk diskConfig(Compute compute, String instanceName) throws  IOException{
        AttachedDisk disk = new AttachedDisk();
        disk.setBoot(true);
        disk.setAutoDelete(true);
        disk.setType("PERSISTENT");

        AttachedDiskInitializeParams initializeParams = new AttachedDiskInitializeParams();
        initializeParams.setDiskName(instanceName);

        imageList(compute);

        return disk;
    }

    public ImageList imageList(Compute compute) throws IOException {
        ImageList imageList;
        String imageProjectId = "ubuntu-os-cloud";

        Compute.Images.List images = compute.images().list(imageProjectId)
                .setFilter("status=READY");
        imageList = images.execute();

        List<Image> images1 = Optional.ofNullable(imageList.getItems())
                .orElseGet(ArrayList::new);

        images1.forEach(System.out::println);

//        List<Instance> instancesItem = Optional
//                .ofNullable(instanceList.getItems())
//                .orElseGet(ArrayList::new);

        return imageList;
    }


}
