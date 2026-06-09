package it.unibo.sap.delivery.application.fleet;

import java.time.LocalDateTime;
import java.util.List;

public final class FleetViews {

    private FleetViews() {
    }

    public record FleetDroneView(String droneId,
                                 String status,
                                 double latitude,
                                 double longitude,
                                 boolean carryingPackage) {
    }

    public record ScheduledDeliveryView(String droneId,
                                        String deliveryId,
                                        LocalDateTime scheduledAt,
                                        String status) {
    }

    public interface FleetMonitoring {
        List<FleetDroneView> snapshot();
    }
}
