package it.unibo.sap.delivery.application;

import it.unibo.sap.common.hexagonal.OutputPort;

public interface TrackingSessionEventObserver extends OutputPort {

    /**
     * Push a tracking update for a delivery.
     *
     * @param deliveryId                    the tracked delivery
     * @param status                        current delivery status (string form)
     * @param latitude                      current drone latitude
     * @param longitude                     current drone longitude
     * @param estimatedTimeRemainingSeconds ETR in seconds (0 once delivered)
     */
    void pushTrackingUpdate(String deliveryId,
                            String status,
                            double latitude,
                            double longitude,
                            long estimatedTimeRemainingSeconds);
}
