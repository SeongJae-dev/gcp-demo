package gcp.example.gcpdemo.util;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Component
public class GcpComputeExample {

    //  private static final String PROJECT_ID = "YOUR_PROJECT_ID";
    private static final String PROJECT_ID = "flash-precept-306501";

    /**
     * Set Compute Engine zone.
     */
    private static final String ZONE_NAME = "us-central1-a";

    /**
     * Set the name of the sample VM instance to be created.
     */
    private static final String SAMPLE_INSTANCE_NAME = "my-sample-instance";

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
        try {
            //인증
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredentials credential = GoogleCredentials.getApplicationDefault();
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credential);


            String applicationName = "gcpDemoApplication";

            //compute 객체 생성
            Compute compute = new Compute.Builder(httpTransport, JSON_FACTORY, requestInitializer)
                    .setApplicationName(applicationName)
                    .build();

            //project에 생성되어 있는 instance list 가져옴
            printInstances(compute);

            //해당 zone에 존재하는 머신 리스트 정보 가져옴
            MachineTypeList machineList = machineList(compute);

            //머신명이 e2인것만 가져옴
            List<MachineType> e2List = machineList.getItems()
                    .stream()
                    .filter(machineType -> machineType.getName().contains("e2"))
                    .collect(Collectors.toList());

//            e2List.stream().forEach(System.out::println);
            //가장 작은 머신타입
            String e2MachineTypeSelfLink = e2List
                    .stream()
                    .filter(machineType -> machineType.getName().contains("small"))
                    .filter(machineType -> (machineType.getGuestCpus() <= 2 && machineType.getMemoryMb() <= 4096))
                    .map(MachineType::getSelfLink)
                    .collect(Collectors.joining());

            log.info("e2MachineTypeSelfLink -> {}", e2MachineTypeSelfLink);

            List<Network> networkList = networkList(compute).getItems();
            networkList.stream().forEach(System.out::println);


            Operation operation = startInstance(compute, SAMPLE_INSTANCE_NAME, e2MachineTypeSelfLink);
            Operation.Error error = blockUntilComplete(compute, operation, OPERATION_TIMEOUT_MILLIS);

            if (error == null) {
                System.out.println("Success!");
            } else {
                System.out.println(error.toPrettyString());
            }

        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }

    }


    // [START list_instances]

    /**
     * Print available machine instances.
     *
     * @param compute The main API access point
     * @return {@code true} if the instance created by this sample app is in the list
     */
    public static boolean printInstances(Compute compute) throws IOException {
        System.out.println("================== Listing Compute Engine Instances ==================");
        Compute.Instances.List instances = compute.instances().list(PROJECT_ID, ZONE_NAME);
        InstanceList list = instances.execute();
        System.out.println("Instances List -> " + list);
        boolean found = false;
        if (list.getItems() == null) {
            System.out.println(
                    "No instances found. Sign in to the Google Developers Console and create "
                            + "an instance at: https://console.developers.google.com/");
        } else {
            for (Instance instance : list.getItems()) {
                System.out.println(instance.toPrettyString());
                if (instance.getName().equals(SAMPLE_INSTANCE_NAME)) {
                    found = true;
                }
            }
        }
        return found;
    }

    public static Compute createComputeService() throws IOException, GeneralSecurityException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        GoogleCredential credential = GoogleCredential.getApplicationDefault();
        if (credential.createScopedRequired()) {
            credential =
                    credential.createScoped(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
        }

        return new Compute.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("Google-ComputeSample/0.1")
                .build();
    }

    public static MachineTypeList machineList(Compute compute) throws IOException {

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

    public static NetworkList networkList(Compute compute) throws IOException {

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


    // [START create_instances]
//    public static void startInstance(Compute compute, String instanceName, String machineTypeSelfLink) throws IOException {
    public static Operation startInstance(Compute compute, String instanceName, String machineTypeSelfLink) throws IOException {
        System.out.println("================== Starting New Instance ==================");

        // Create VM Instance object with the required properties.
        Instance instance = new Instance();
        instance.setName(instanceName);
        instance.setMachineType(machineTypeSelfLink);
        // Add Network Interface to be used by VM Instance.
        NetworkInterface ifc = new NetworkInterface();

        ifc.setNetwork(
                String.format(
                        "https://www.googleapis.com/compute/v1/projects/%s/global/networks/default",
                        PROJECT_ID));
        List<AccessConfig> configs = new ArrayList<>();
        AccessConfig config = new AccessConfig();
        config.setType(NETWORK_INTERFACE_CONFIG);
        config.setName(NETWORK_ACCESS_CONFIG);
        configs.add(config);
        ifc.setAccessConfigs(configs);
        instance.setNetworkInterfaces(Collections.singletonList(ifc));

        // Add attached Persistent Disk to be used by VM Instance.
        AttachedDisk disk = new AttachedDisk();
        disk.setBoot(true);
        disk.setAutoDelete(true);
        disk.setType("PERSISTENT");
        AttachedDiskInitializeParams params = new AttachedDiskInitializeParams();
        // Assign the Persistent Disk the same name as the VM Instance.
        params.setDiskName(instanceName);
        // Specify the source operating system machine image to be used by the VM Instance.
        params.setSourceImage(SOURCE_IMAGE_PREFIX + SOURCE_IMAGE_PATH);
        // Specify the disk type as Standard Persistent Disk
        params.setDiskType(
                String.format(
                        "https://www.googleapis.com/compute/v1/projects/%s/zones/%s/diskTypes/pd-standard",
                        PROJECT_ID, ZONE_NAME));
        disk.setInitializeParams(params);
        instance.setDisks(Collections.singletonList(disk));

        // Initialize the service account to be used by the VM Instance and set the API access scopes.
        ServiceAccount account = new ServiceAccount();
        account.setEmail("default");
        List<String> scopes = new ArrayList<>();
        scopes.add("https://www.googleapis.com/auth/devstorage.full_control");
        scopes.add("https://www.googleapis.com/auth/compute");
        account.setScopes(scopes);
        instance.setServiceAccounts(Collections.singletonList(account));

        // Optional - Add a startup script to be used by the VM Instance.
        Metadata meta = new Metadata();
        Metadata.Items item = new Metadata.Items();
        item.setKey("startup-script-url");
        // If you put a script called "vm-startup.sh" in this Google Cloud Storage
        // bucket, it will execute on VM startup.  This assumes you've created a
        // bucket named the same as your PROJECT_ID.
        // For info on creating buckets see:
        // https://cloud.google.com/storage/docs/cloud-console#_creatingbuckets
        item.setValue(String.format("gs://%s/vm-startup.sh", PROJECT_ID));
        meta.setItems(Collections.singletonList(item));
        instance.setMetadata(meta);

        System.out.println(instance.toPrettyString());
        Compute.Instances.Insert insert = compute.instances().insert(PROJECT_ID, ZONE_NAME, instance);
        return insert.execute();
    }

    public static Operation.Error blockUntilComplete(
            Compute compute, Operation operation, long timeout) throws Exception {
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
