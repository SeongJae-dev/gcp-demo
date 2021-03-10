package gcp.example.gcpdemo.service.gcp;

import com.google.api.services.compute.Compute;

import java.io.IOException;

public interface BaseConfigure {
    public Object config(String applicationName) throws IOException;
    public boolean update(String applicationName) throws Exception;
    public boolean insert(String applicationName) throws Exception;
    public boolean delete(String applicationName, String resourceName) throws Exception;
}
