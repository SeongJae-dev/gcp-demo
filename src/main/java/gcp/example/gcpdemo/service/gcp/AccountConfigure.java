package gcp.example.gcpdemo.service.gcp;

import com.google.api.services.compute.model.ServiceAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class AccountConfigure extends OperationErrorHandle{

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
}
