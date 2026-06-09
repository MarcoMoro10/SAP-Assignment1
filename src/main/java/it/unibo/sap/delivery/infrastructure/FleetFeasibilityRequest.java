package it.unibo.sap.delivery.infrastructure;

public record FleetFeasibilityRequest(String deliveryId,
                                      double weightKg,
                                      double pickupLatitude,
                                      double pickupLongitude,
                                      double destinationLatitude,
                                      double destinationLongitude,
                                      long deadlineMinutes) {
}
