package gcp.example.gcpdemo.service.gcp;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DiskConfigure extends OperationErrorHandle{

    @Autowired
    private ImageConfigure imageConfigure;

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

    public AttachedDisk diskConfig(Compute compute, String instanceName) throws IOException {
        log.info("================== GCP diskConfig ==================");
        AttachedDisk disk = new AttachedDisk();
        disk.setBoot(true);
        disk.setAutoDelete(true);
        disk.setType("PERSISTENT");

        AttachedDiskInitializeParams initializeParams = new AttachedDiskInitializeParams();
        initializeParams.setDiskName(instanceName);
        initializeParams.setSourceImage(imageConfigure.selectImageName(compute));
        initializeParams.setDiskType(
                String.format(
                        "https://www.googleapis.com/compute/v1/projects/%s/zones/%s/diskTypes/pd-standard",
                        PROJECT_ID, ZONE_NAME));
        disk.setInitializeParams(initializeParams);

        return disk;
    }



}
