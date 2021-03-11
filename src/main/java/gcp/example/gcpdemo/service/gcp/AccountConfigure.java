package gcp.example.gcpdemo.service.gcp;

import com.google.api.services.compute.model.ServiceAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class AccountConfigure extends OperationErrorHandle implements BaseConfigure{


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

    @Override
    public Object config() throws IOException {
        return null;
    }

    @Override
    public boolean update(String applicationName) throws Exception {
        return false;
    }

    @Override
    public boolean insert(String applicationName) throws Exception {
        return false;
    }

    @Override
    public boolean delete(String applicationName, String resourceName) throws Exception {
        return false;
    }
}
