package gcp.example.gcpdemo.service;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.MachineType;
import com.google.api.services.compute.model.MachineTypeList;
import com.google.api.services.compute.model.NetworkList;
import gcp.example.gcpdemo.service.gcp.*;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class GcpComputeServiceTest {

    static Compute compute;
    static String applicationName = "gcpDemoApplication";
    static String instanceName = "gcp-demo-instance00";

    @Autowired
    GcpComputeService gcpComputeService;

    @Autowired
    ComputeService computeService;
    
    @Autowired
    InstanceConfigure instanceConfigure;

    @Autowired
    FirewallConfigure firewallConfigure;

    @Autowired
    NetworkConfigure networkConfigure;

    @Autowired
    AddressesConfig addressesConfig;

    @BeforeEach
    void init() throws GeneralSecurityException, IOException {
        System.out.println("init");
        compute = gcpComputeService.createCompute(applicationName);
    }

    @Test
    void testCreateInstance() throws Exception {
        boolean isCreated = computeService.createComputeInstance(instanceName, applicationName);
        assertEquals(isCreated, true);
    }


    @Test
    void testUpdateInstance() throws Exception {
        boolean b = gcpComputeService.updateInstance(instanceName, applicationName);
        assertEquals(true, b);
    }

    @Test
    void testDeleteInstance() throws Exception {
        boolean b = instanceConfigure.deleteInstance(instanceName, applicationName);
//        boolean b = gcpComputeService.deleteInstance(instanceName, applicationName);
        assertTrue(b);
    }

    @Test
    void testGcpInstanceList() throws Exception {
        List<Instance> instanceItems = gcpComputeService.listInstance(compute);

        System.out.println(instanceItems);
        assertEquals(false, instanceItems.isEmpty());
    }

    @Test
    void testImage() throws Exception {
        String osName = gcpComputeService.selectImageName(compute);
        System.out.println(osName);
        assertTrue(osName.contains("ubuntu"));
    }

    @Test
    void testDiskType() throws Exception{
        MachineTypeList machineTypeList = gcpComputeService.machineTypeList(compute);
        System.out.println(machineTypeList);
        assertEquals(false, machineTypeList.isEmpty());

    }

    @Test
    void testNetwork() throws Exception{

        NetworkList networkList = gcpComputeService.networkList(compute);
        networkList.getItems().forEach(System.out::println);
    }

    @Test
    void testMachineTypeList() throws Exception{
         gcpComputeService.machineTypeList(compute)
                .getItems()
                .stream()
                .filter(machineType -> (machineType.getName().contains("e2")))
                .collect(Collectors.toList())
                .forEach(System.out::println);
    }

    @Test
    void testCustomMachineType() throws  Exception{
        MachineType machineType = new MachineType();
        machineType.setGuestCpus(2);
        machineType.setMemoryMb(2048);
        String s = gcpComputeService.customMachineTypeSelfLink(machineType);
        System.out.println(s);
    }

    @Test
    void testFirewall() throws  Exception{
//        boolean res = firewallConfigure.config().insert(applicationName);
        boolean res2 = firewallConfigure.config().update(applicationName);
//        firewallConfigure.update(applicationName,"default-allow-mysql");
//        boolean res = firewallConfigure.delete(applicationName,"default-allow-mysql");
//        assertFalse(res);
        assertFalse(res2);
    }

    @Test
    void testAutoIpToFixedIp() throws  Exception{
        String instanceNatIp = instanceConfigure.getInstanceNatIp();
        System.out.println(instanceNatIp);
        boolean insert = addressesConfig.config("fix-ip-01").insert(applicationName);
        assertFalse(insert);

    }




}