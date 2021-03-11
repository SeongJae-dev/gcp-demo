package gcp.example.gcpdemo.service.gcp;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Address;
import com.google.api.services.compute.model.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Component
public class AddressesConfig  extends OperationErrorHandle implements BaseConfigure{

    @Autowired
    NetworkConfigure networkConfigure;
    @Autowired
    InstanceConfigure instanceConfigure;

    Address address = new Address();

    Compute compute;

    public AddressesConfig() throws GeneralSecurityException, IOException {
        this.compute = new ComputeService().createCompute(applicationName);
    }

    @Override
    public Object config() throws IOException {


        return null;
    }

    public AddressesConfig config(String addressName) throws IOException {

        address.setName(addressName);
        address.setAddress(instanceConfigure.getInstanceNatIp());

        return this;
    }

    @Override
    public boolean update(String applicationName) throws Exception {
        return false;
    }

    @Override
    public boolean insert(String applicationName) throws Exception {
        Compute.Addresses.Insert insert = compute.addresses().insert(PROJECT_ID, REGION, address);
        Operation execute = insert.execute();

        return execute.isEmpty();
    }

    @Override
    public boolean delete(String applicationName, String resourceName) throws Exception {
        return false;
    }
}
