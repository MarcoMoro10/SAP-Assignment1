package it.unibo.sap.delivery.application;

import it.unibo.sap.common.ddd.Repository;
import it.unibo.sap.common.hexagonal.OutputPort;
import it.unibo.sap.delivery.domain.deliveries.Delivery;
import it.unibo.sap.delivery.domain.deliveries.DeliveryId;
import it.unibo.sap.delivery.domain.deliveries.SenderId;

import java.util.List;

public interface DeliveryRepository extends Repository<DeliveryId, Delivery>, OutputPort {

    List<Delivery> findBySender(SenderId senderId);
}
