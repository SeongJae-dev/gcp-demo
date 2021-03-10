package gcp.example.gcpdemo.service.gcp;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class InstanceConfigure extends OperationErrorHandle{

    @Autowired
    NetworkConfigure networkConfigure;

    @Autowired
    ComputeService computeService;

    @Autowired
    AccountConfigure accountConfigure;

    @Autowired
    DiskConfigure diskConfigure;




    public Operation createInstance(Compute compute, String instanceName, String machineTypeSelfLink) throws IOException {
        log.info("================== Starting Create Instance ================== -> {}", instanceName);

        //instance 설정
        Instance instance = new Instance();
        instance.setName(instanceName);
        instance.setMachineType(machineTypeSelfLink);

        //NetWork interface 설정
        instance.setNetworkInterfaces(Collections.singletonList(networkConfigure.networkInterfaceConfig(compute)));
        //Disk 설정
        instance.setDisks(Collections.singletonList(diskConfigure.diskConfig(compute, instanceName)));
        //account 설정
        instance.setServiceAccounts(Collections.singletonList(accountConfigure.accountConfig()));
        //mateData 설정 (script, ssh)
        instance.setMetadata(metadataConfig());

        Compute.Instances.Insert insert = compute.instances().insert(PROJECT_ID, ZONE_NAME, instance);
        return insert.execute();
    }

    public boolean deleteInstance(String instanceName, String applicationName) throws Exception {
        log.info("================== Starting Delete Instance ================== -> {}", instanceName);
        Compute compute = computeService.createCompute(applicationName);
        Compute.Instances.Delete delete =
                compute.instances().delete(PROJECT_ID, ZONE_NAME, instanceName);

        Operation operation = delete.execute();
        Operation.Error error = blockUntilComplete(compute, operation);

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
        Compute compute = computeService.createCompute(applicationName);
        InstancesSetLabelsRequest requestBody = new InstancesSetLabelsRequest();
        Map<String, String> label = new HashMap<>();
        label.put("environment","production");
        requestBody.setLabels(label);
        requestBody.setLabelFingerprint(getInstanceLabelFingerprint(applicationName));

        Compute.Instances.SetLabels setLabels = compute.instances().setLabels(PROJECT_ID, ZONE_NAME, getInstanceName(applicationName), requestBody);

        Operation execute = setLabels.execute();
        Operation.Error error = blockUntilComplete(compute, execute);

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

    public Metadata metadataConfig() {
        Metadata metadata = new Metadata();
        List<Metadata.Items> items = new ArrayList<>();
        Metadata.Items item = new Metadata.Items();
        item.setKey("startup-script");
        item.setValue(createUserScript("test"));
        items.add(item);
        items.add(createSshSettings());
        metadata.setItems(items);
        return metadata;
    }

    public String createUserScript(String id) {
        return "sudo useradd -m " + id;
    }

    public Metadata.Items createSshSettings(){
        Metadata.Items item = new Metadata.Items();
        StringBuilder builder = new StringBuilder();
        builder.append("");
        item.setKey("ssh-keys");
        item.setValue(builder.toString());

        return item;

    }

    public String getInstanceLabelFingerprint(String applicationName) throws GeneralSecurityException, IOException {
        Compute compute = computeService.createCompute(applicationName);
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
        Compute compute = computeService.createCompute(applicationName);
        List<Instance> instances = listInstance(compute);
        log.info("existInstance -> {}", instances);
        String instanceName = instances.stream()
                .map(Instance::getName)
                .limit(1)
                .collect(Collectors.joining());

        // 해당 zone에 존재하는 머신 리스트 정보 가져옴
        return instanceName;
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

}
