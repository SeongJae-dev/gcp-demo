package gcp.example.gcpdemo.client;


import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class GcpClient {

    private final WebClient webClient;

    public GcpClient(WebClient webClient) {
        this.webClient = webClient;
    }

//    public void getTemplateList(){
//        webClient
//                .get()
//    }

    public static void main(String[] args) {
//        List<String> list = Arrays.asList("e2-medium","e2-micro","e2-small","c2-standard-8");
//        List<String> e2 = list.stream().filter(s -> s.contains("e2")).collect(Collectors.toList());
//
//        e2.stream().filter(s -> s.contains("small")).forEach(System.out::println);

        String testTime = "2021-02-24T10:31:51.964-08:00";

//        LocalDateTime localDateTime = LocalDateTime.parse(testTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
//        System.out.println(localDateTime);



    }
}
