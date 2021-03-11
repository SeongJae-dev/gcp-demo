package gcp.example.gcpdemo.service.gcp;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.Operation;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class FirewallConfigure extends  OperationErrorHandle implements BaseConfigure{

    private Firewall firewall;
    public Compute compute;

    @Autowired
    ComputeService computeService;

    public FirewallConfigure() throws GeneralSecurityException, IOException {
        this.compute = new ComputeService().createCompute(applicationName);
    }

    @SneakyThrows
    @Override
    public FirewallConfigure config() throws IOException {
        log.info("================== GCP firewallConfig ==================");
        Firewall firewall = new Firewall();
        List<String> sourceRanges = new ArrayList<>();
        List<String> targetTags = new ArrayList<>();
        sourceRanges.add("0.0.0.0/0");
        firewall.setName("default-allow-mysql");
        // 미지정시 default
//        firewall.setNetwork();
        // 우선순위 0~65535 미지정 1000 기본값, 암묵적으로 65535 사용으로 충돌방지 권장
        firewall.setPriority(65535);
        //방화벽 규칙 적용 대상 타겟설정 지정하지 않으면 vpc 네트워크 모든 인스턴스 대상 적용, 지정하면 태그 지정된 네트워크
//        firewall.setTargetTags();
        //소스 범위 CIDR 형태로 사용 IPv4 만 지원 0.0.0.0/0
        firewall.setSourceRanges(sourceRanges);
        //프로토콜 및 포트 설정
        firewall.setAllowed(allowedConfig("tcp", new String[]{"3306"}));
        firewall.setDescription("mysql allow");
        //방화벽 적용 타겟 설정
//        targetTags.add("test");
//        firewall.setTargetTags(targetTags);
        this.firewall = firewall;
        this.compute = computeService.createCompute(applicationName);
        return this;
    }
    @Override
    public boolean update(String applicationName) throws Exception{
        config();
        Compute.Firewalls.Update update = this.compute.firewalls().update(PROJECT_ID, this.firewall.getName(), this.firewall);
        Operation response = update.execute();
        log.info("firewall update res -> {}", response);

        return response.isEmpty();

    }

    public boolean update(String applicationName,String resourceName) throws Exception{
        config();
        Compute.Firewalls.Update update = this.compute.firewalls().update(PROJECT_ID, resourceName, this.firewall);
        Operation response = update.execute();
        log.info("firewall update res -> {}", response);

        return response.isEmpty();

    }



    @Override
    public boolean insert(String applicationName) throws Exception {
        Compute.Firewalls.Insert insert = this.compute.firewalls().insert(PROJECT_ID, firewall);
        Operation response = insert.execute();
        log.info("firewall insert res -> {}", response);

        return response.isEmpty();
    }

    @Override
    public boolean delete(String applicationName, String resourceName) throws Exception {
        Compute.Firewalls.Delete delete = this.compute.firewalls().delete(PROJECT_ID, resourceName);
        Operation response = delete.execute();
        log.info("firewall delete res -> {}", response);

        return response.isEmpty();
    }

    public List<Firewall.Allowed> allowedConfig(String protocol, String[] ports){
        List<Firewall.Allowed> allowedList = new ArrayList<>();
        //이 값은 다음 중 한 잘 알려진 프로토콜 문자열 (중 하나가 될 수 있습니다 tcp, udp, icmp, esp, ah, ipip, sctp)
        Firewall.Allowed allowed = new Firewall.Allowed();
        allowed.setIPProtocol(protocol);
        //port 정책은 List<String> 형태에 담아 allowed에 추가
        List<String> port = Arrays.asList(ports);
        allowed.setPorts(port);
        allowedList.add(allowed);

        log.info("Firewall Allowed ->{}", allowedList);
        return allowedList;
    }

    public List<Firewall.Denied> deniedConfig(String protocol, String[] ports){
        List<Firewall.Denied> deniedList = new ArrayList<>();
        //이 값은 다음 중 한 잘 알려진 프로토콜 문자열 (중 하나가 될 수 있습니다 tcp, udp, icmp, esp, ah, ipip, sctp)
        Firewall.Denied denied = new Firewall.Denied();
        denied.setIPProtocol(protocol);
        //port 정책은 List<String> 형태에 담아 Denied 추가
        List<String> port = Arrays.asList(ports);
        denied.setPorts(port);
        deniedList.add(denied);

        log.info("Firewall Denied ->{}", deniedList);
        return deniedList;
    }
}
