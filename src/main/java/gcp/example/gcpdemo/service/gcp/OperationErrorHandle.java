package gcp.example.gcpdemo.service.gcp;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Operation;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class OperationErrorHandle {

    //  private static final String PROJECT_ID = "YOUR_PROJECT_ID";
     public static  String PROJECT_ID = "flash-precept-306501";

    /**
     * Set Compute Engine zone.
     */
    public static  String ZONE_NAME = "us-central1-a";

    private static final long OPERATION_TIMEOUT_MILLIS = 60 * 1000;


    public static Operation.Error blockUntilComplete(Compute compute, Operation operation) throws Exception {
        long start = System.currentTimeMillis();
        final long pollInterval = 5 * 1000;
        String zone = operation.getZone(); // null for global/regional operations
        if (zone != null) {
            String[] bits = zone.split("/");
            zone = bits[bits.length - 1];
        }
        String status = operation.getStatus();
        String opId = operation.getName();
        while (operation != null && !status.equals("DONE")) {
            Thread.sleep(pollInterval);
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= OPERATION_TIMEOUT_MILLIS) {
                throw new InterruptedException("Timed out waiting for operation to complete");
            }
            System.out.println("waiting...");
            if (zone != null) {
                Compute.ZoneOperations.Get get = compute.zoneOperations().get(PROJECT_ID, zone, opId);
                operation = get.execute();
            } else {
                Compute.GlobalOperations.Get get = compute.globalOperations().get(PROJECT_ID, opId);
                operation = get.execute();
            }
            if (operation != null) {
                status = operation.getStatus();
            }
        }
        return operation == null ? null : operation.getError();
    }
}
