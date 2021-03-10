package gcp.example.gcpdemo.service.gcp;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DiskConfigure extends OperationErrorHandle{


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

}
