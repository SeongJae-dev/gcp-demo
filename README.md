# GCP-Demo

> GCP Cloud SDK 연동하여 GCP에 Computer Instance를 조작 및 관리하는 데모 프로젝트 입니다. 
---
## Computer Instance  Test

    public Compute createCompute(String applicationName) throws GeneralSecurityException, IOException {
        log.info("================== GCP Compute Build ==================");

        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleCredentials credential = GoogleCredentials.getApplicationDefault();
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credential);

        return new Compute.Builder(httpTransport, JSON_FACTORY, requestInitializer)
                .setApplicationName(applicationName)
                .build();

    }
---

## Network Test
  
    public NetworkInterface networkInterfaceConfig(Compute compute) throws IOException {
        log.info("================== GCP networkInterfaceConfig ==================");
        NetworkInterface networkInterface = new NetworkInterface();
        String networkSelfLink = networkList(compute).getItems()
                .stream()
                .map(Network::getSelfLink)
                .collect(Collectors.joining());

        networkInterface.setNetwork(networkSelfLink);

        List<AccessConfig> configs = new ArrayList<>();
        AccessConfig config = new AccessConfig();
        config.setType(NETWORK_INTERFACE_CONFIG);
        config.setName(NETWORK_ACCESS_CONFIG);
        configs.add(config);
        networkInterface.setAccessConfigs(configs);

        return networkInterface;
    }
---    

## Disk Test

     public AttachedDisk diskConfig(Compute compute, String instanceName) throws IOException {
        log.info("================== GCP diskConfig ==================");
        AttachedDisk disk = new AttachedDisk();
        disk.setBoot(true);
        disk.setAutoDelete(true);
        disk.setType("PERSISTENT");

        AttachedDiskInitializeParams initializeParams = new AttachedDiskInitializeParams();
        initializeParams.setDiskName(instanceName);
        initializeParams.setSourceImage(imageConfigure.selectImageName(compute));
        initializeParams.setDiskType(
                String.format(
                        "https://www.googleapis.com/compute/v1/projects/%s/zones/%s/diskTypes/pd-standard",
                        PROJECT_ID, ZONE_NAME));
        disk.setInitializeParams(initializeParams);

        return disk;
    }

## Image Test

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



---

## Firewall Test

     public FirewallConfigure config() throws IOException {
        log.info("================== GCP firewallConfig ==================");
        Firewall firewall = new Firewall();
        List<String> sourceRanges = new ArrayList<>();
        List<String> targetTags = new ArrayList<>();
        sourceRanges.add("0.0.0.0/0");
        firewall.setName("default-allow-mysql");
        // firewall.setNetwork();
        // 우선순위 0~65535 미지정 1000 기본값, 암묵적으로 65535 사용으로 충돌방지 권장
        firewall.setPriority(65535);
        // 소스 범위 CIDR 형태로 사용 IPv4 만 지원 0.0.0.0/0
        firewall.setSourceRanges(sourceRanges);
        // 프로토콜 및 포트 설정
        firewall.setAllowed(allowedConfig("tcp", new String[]{"3306"}));
        firewall.setDescription("mysql allow");
        // 방화벽 적용 타겟 설정
        targetTags.add("test");
        firewall.setTargetTags(targetTags);
        this.firewall = firewall;
        this.compute = computeService.createCompute(applicationName);
        return this;
    }
  
---
API Source 
 - GCP SDK : https://cloud.google.com/sdk
 - GCP Api Reperance : https://cloud.google.com/compute/docs/reference/rest/v1?authuser=1
