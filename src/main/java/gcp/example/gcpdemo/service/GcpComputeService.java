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
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
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

    private static final long OPERATION_TIMEOUT_MILLIS = 60 * 1000;

    public static void main(String[] args) throws GeneralSecurityException, Exception {
        GcpComputeService gcpComputeService = new GcpComputeService();

        String applicationName = "gcpDemoApplication";
        String instanceName = "gcp-demo-instance00";

//        //인스턴스 정보 불러오기
//        gcpComputeService.getInstance(applicationName);
//        //인스턴스 생성
//        gcpComputeService.createComputeInstance(instanceName, applicationName);
        //인스턴스 삭제
        gcpComputeService.deleteInstance(instanceName,applicationName);
        //인스턴스 수정(라벨)
//        gcpComputeService.updateInstance(instanceName,applicationName);

    }

    public String getInstanceLabelFingerprint(String applicationName) throws GeneralSecurityException, IOException {
        Compute compute = createCompute(applicationName);
        List<Instance> instances = listInstance(compute);
        log.info("existInstance -> {}", instances);
        String instanceName = instances.stream()
                .map(Instance::getLabelFingerprint)
                .limit(1)
                .collect(Collectors.joining());


//        log.info(collect);
        // 해당 zone에 존재하는 머신 리스트 정보 가져옴
        return instanceName;
    }
    public String getInstanceName(String applicationName) throws GeneralSecurityException, IOException {
        Compute compute = createCompute(applicationName);
        List<Instance> instances = listInstance(compute);
        log.info("existInstance -> {}", instances);
        String instanceName = instances.stream()
                .map(Instance::getName)
                .limit(1)
                .collect(Collectors.joining());

        // 해당 zone에 존재하는 머신 리스트 정보 가져옴
        return instanceName;
    }

    public String customMachineTypeSelfLink(MachineType machineType) throws Exception{
        String customSelfLink = "https://www.googleapis.com/compute/v1/projects/flash-precept-306501/zones/us-central1-a/machineTypes/custom";
        StringBuilder builder = new StringBuilder(customSelfLink);
        builder.append("-").append(machineType.getGuestCpus());
        builder.append("-").append(machineType.getMemoryMb());
        machineType.setSelfLink(customSelfLink);

        return  builder.toString();

    }


    public boolean createComputeInstance(String instanceName, String applicationName) throws Exception, IOException, GeneralSecurityException {

        Compute compute = createCompute(applicationName);
        String e2MachineTypeSelfLink = machineTypeList(compute).getItems()
                .stream()
                .filter(machineType -> (machineType.getName().contains("e2") && machineType.getName().contains("small")))
                .filter(machineType -> (machineType.getGuestCpus() <= 2 && machineType.getMemoryMb() <= 4096))
                .map(MachineType::getSelfLink)
                .collect(Collectors.joining());

//        MachineType machineType = new MachineType();
//        machineType.setGuestCpus(2);
//        machineType.setMemoryMb(4096);

//        Operation operation = createInstance(compute, instanceName, customMachineTypeSelfLink(machineType));
        Operation operation = createInstance(compute, instanceName, e2MachineTypeSelfLink);
        Operation.Error error = blockUntilComplete(compute, operation, OPERATION_TIMEOUT_MILLIS);

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

    public Operation createInstance(Compute compute, String instanceName, String machineTypeSelfLink) throws IOException {
        log.info("================== Starting Create Instance ================== -> {}", instanceName);

        //instance 설정
        Instance instance = new Instance();
        instance.setName(instanceName);
        instance.setMachineType(machineTypeSelfLink);

        //NetWork interface 설정
        instance.setNetworkInterfaces(Collections.singletonList(networkInterfaceConfig(compute)));
        //Disk 설정
        instance.setDisks(Collections.singletonList(diskConfig(compute, instanceName)));
        //account 설정
        instance.setServiceAccounts(Collections.singletonList(accountConfig()));

        Compute.Instances.Insert insert = compute.instances().insert(PROJECT_ID, ZONE_NAME, instance);
        return insert.execute();
    }

    public boolean deleteInstance(String instanceName, String applicationName) throws Exception {
        log.info("================== Starting Delete Instance ================== -> {}", instanceName);
        Compute compute = createCompute(applicationName);
        Compute.Instances.Delete delete =
                compute.instances().delete(PROJECT_ID, ZONE_NAME, instanceName);

        Operation operation = delete.execute();
        Operation.Error error = blockUntilComplete(compute, operation, OPERATION_TIMEOUT_MILLIS);

        boolean isDeleted;
        if (error == null) {
            log.info("Delete Instance Success!");
            isDeleted = true;
        } else {
            log.info(error.toPrettyString());
            isDeleted = false;
        }
        return isDeleted;
    }

    public boolean updateInstance(String instanceName, String applicationName) throws Exception {
        log.info("================== Starting Update Instance ================== -> {}", instanceName);
        Compute compute = createCompute(applicationName);
        InstancesSetLabelsRequest requestBody = new InstancesSetLabelsRequest();
        Map<String, String> label = new HashMap<>();
        label.put("environment","production");
        requestBody.setLabels(label);
        requestBody.setLabelFingerprint(getInstanceLabelFingerprint(applicationName));

        Compute.Instances.SetLabels setLabels = compute.instances().setLabels(PROJECT_ID, ZONE_NAME, getInstanceName(applicationName), requestBody);

        Operation execute = setLabels.execute();
        Operation.Error error = blockUntilComplete(compute, execute, OPERATION_TIMEOUT_MILLIS);

        boolean isUpdated;
        if (error == null) {
            log.info("Update Instance Success!");
            isUpdated = true;
        } else {
            log.info(error.toPrettyString());
            isUpdated = false;
        }
        return isUpdated;

    }

    public Compute createCompute(String applicationName) throws GeneralSecurityException, IOException {
        log.info("================== GCP Compute Build ==================");

        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredentials credential = GoogleCredentials.getApplicationDefault();
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credential);

        return new Compute.Builder(httpTransport, JSON_FACTORY, requestInitializer)
                .setApplicationName(applicationName)
                .build();

    }

    public List<Instance> listInstance(Compute compute) throws IOException {
        log.info("================== GCP Compute Instance List ==================");

        Compute.Instances.List instances = compute.instances().list(PROJECT_ID, ZONE_NAME);
        InstanceList instanceList = instances.execute();
        log.info("instanceList -> {} ", instanceList);
//        boolean existInstance = false;
        List<Instance> instancesItem = Optional
                .ofNullable(instanceList.getItems())
                .orElseGet(ArrayList::new);

        log.info("instancesItem -> {}", instancesItem);

//        return instancesItem.size() >= 1;
        return instancesItem;
    }

    public MachineTypeList machineTypeList(Compute compute) throws IOException {
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

    public AttachedDisk diskConfig(Compute compute, String instanceName) throws IOException {
        log.info("================== GCP diskConfig ==================");
        AttachedDisk disk = new AttachedDisk();
        disk.setBoot(true);
        disk.setAutoDelete(true);
        disk.setType("PERSISTENT");

        AttachedDiskInitializeParams initializeParams = new AttachedDiskInitializeParams();
        initializeParams.setDiskName(instanceName);
        initializeParams.setSourceImage(selectImageName(compute));
        initializeParams.setDiskType(
                String.format(
                        "https://www.googleapis.com/compute/v1/projects/%s/zones/%s/diskTypes/pd-standard",
                        PROJECT_ID, ZONE_NAME));
        disk.setInitializeParams(initializeParams);

        return disk;
    }

    public String selectImageName(Compute compute) throws IOException {
        log.info("================== GCP selectImageName ==================");
        ImageList imageList;
        String imageProjectId = "ubuntu-os-cloud";

        Compute.Images.List images = compute.images().list(imageProjectId)
                .setFilter("status=READY");

        do {
            imageList = images.execute();
            if (imageList.getItems() == null) {
                continue;
            }
            images.setPageToken(imageList.getNextPageToken());
        } while (imageList.getNextPageToken() != null);

        return getOSImage(imageList);
    }

    public String getOSImage(ImageList imageList) {
        log.info("================== GCP getOSImage ==================");

        return Optional.ofNullable(imageList.getItems())
                .orElseGet(ArrayList::new)
                .stream()
                .map(Image::getSelfLink)
                .limit(1)
                .collect(Collectors.joining());

    }

    public ServiceAccount accountConfig() {
        log.info("================== GCP accountConfig ==================");
        // Initialize the service account to be used by the VM Instance and set the API access scopes.
        ServiceAccount account = new ServiceAccount();
        account.setEmail("default");
        List<String> scopes = new ArrayList<>();
        scopes.add("https://www.googleapis.com/auth/devstorage.full_control");
        scopes.add("https://www.googleapis.com/auth/compute");
        account.setScopes(scopes);

        return account;
    }

    public static Operation.Error blockUntilComplete(Compute compute, Operation operation, long timeout) throws Exception {
        long start = System.currentTimeMillis();
        final long pollInterval = 5 * 1000;
        String zone = operation.getZone(); // null for global/regional operations
        if (zone != null) {
            String[] bits = zone.split("/");
            zone = bits[bits.length - 1];
        }
        String status = operation.getStatus();
        String opId = operation.getName();
        while (operation != null && !status.equals("DONE")) {
            Thread.sleep(pollInterval);
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= timeout) {
                throw new InterruptedException("Timed out waiting for operation to complete");
            }
            System.out.println("waiting...");
            if (zone != null) {
                Compute.ZoneOperations.Get get = compute.zoneOperations().get(PROJECT_ID, zone, opId);
                operation = get.execute();
            } else {
                Compute.GlobalOperations.Get get = compute.globalOperations().get(PROJECT_ID, opId);
                operation = get.execute();
            }
            if (operation != null) {
                status = operation.getStatus();
            }
        }
        return operation == null ? null : operation.getError();
    }


}
