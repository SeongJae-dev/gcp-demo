package gcp.example.gcpdemo.service;

import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.hamcrest.MatcherAssert.assertThat;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class GcpComputeServiceTest {


    @Autowired
    GcpComputeService gcpComputeService;

    static String applicationName = "gcpDemoApplication";
    static String instanceName = "gcp-demo-instance00";

    @Test
    void testCreateInstance() throws Exception {
        boolean isCreated = gcpComputeService.createComputeInstance(instanceName, applicationName);
        assertEquals(isCreated,true);
    }


    @Test
    void testUpdateInstnace() throws Exception{
        boolean b = gcpComputeService.updateInstance(instanceName, applicationName);
        assertEquals(b, true);
    }

    @Test
    void testDeleteInstance() throws Exception{
        boolean b = gcpComputeService.deleteInstance(instanceName, applicationName);
        assertEquals(b, true);
    }



}