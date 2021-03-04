package gcp.example.gcpdemo.client;


import org.springframework.web.reactive.function.client.WebClient;

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
        List<String> list = Arrays.asList("e2-medium","e2-micro","e2-small","c2-standard-8");
        List<String> e2 = list.stream().filter(s -> s.contains("e2")).collect(Collectors.toList());

        e2.stream().filter(s -> s.contains("small")).forEach(System.out::println);

    }
}
