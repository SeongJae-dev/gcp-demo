package gcp.example.gcpdemo.service.gcp;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Image;
import com.google.api.services.compute.model.ImageList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ImageConfigure {

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
