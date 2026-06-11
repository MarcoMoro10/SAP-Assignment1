Feature: Fleet telemetry (internal drone simulation)
  As the Shipping on the Air system,
  I want the internally simulated drones to update their position and status
  so that delivery tracking and fleet monitoring reflect the real state of the fleet.

  # The drone is NOT an external system. Each active drone is simulated inside the
  # delivery-service by a Virtual Thread (DroneSimulator). Position and status updates
  # are INTERNAL domain events of the Fleet Context (Position Updated, Drone Arrived,
  # Drone Out Of Service), propagated IN-PROCESS via the Observer pattern. There is no
  # network ingestion and no message broker.
  #
  # The Deliveries module reacts to those events in-process (Observer) and updates the
  # delivery / tracking read model. "Eventually" below denotes the in-process propagation
  # of an event through its observers within the same process, NOT an asynchronous queue.
  #
  # The "unknown drone" and "invalid position" scenarios test DOMAIN INVARIANTS, not the
  # validation of untrusted network input: a Position value object cannot be constructed
  # with out-of-range coordinates, and the Fleet module rejects an unknown DroneId as a
  # violated internal precondition.
  #
  # Test strategy (see architecture §3): this is an acceptance test of the delivery-service.
  # The Given configures the initial state of the internal Fleet module IN-PROCESS, through
  # the domain (drones created via their legitimate factory/constructor and registered in
  # the in-memory fleet repository). There is no REST endpoint to set or inject fleet state;
  # the Given works against the domain directly because the fleet has no network surface.

  Background:
    Given drone "DRN-1" is a known drone in the fleet
    And drone "DRN-1" is assigned to delivery "DLV-100" in status "IN_PROGRESS"

  Scenario: A valid position update is recorded by the Fleet module
    When drone "DRN-1" updates its position to "44.50, 11.35" with status "IN_DELIVERY"
    Then the update should be applied
    And the current position of drone "DRN-1" should be "44.50, 11.35"
    And a "Position Updated" event should be published for drone "DRN-1"

  Scenario: A valid position update is eventually reflected in delivery tracking
    When drone "DRN-1" updates its position to "44.50, 11.35" with status "IN_DELIVERY"
    Then the update should be applied
    And the tracking view of delivery "DLV-100" should eventually show position "44.50, 11.35"

  Scenario: An update for an unknown drone violates a Fleet precondition
    When an update is issued for an unknown drone "DRN-999" with position "44.50, 11.35" and status "IN_DELIVERY"
    Then the update should be rejected with the error "Unknown drone"
    And no delivery state should be changed

  Scenario: A position with out-of-range coordinates violates the Position invariant
    When drone "DRN-1" attempts to update its position to "999.0, 999.0" with status "IN_DELIVERY"
    Then the update should be rejected with the error "Invalid position"
    And the last known position of drone "DRN-1" should not change

  Scenario: Arrival marks the drone as arrived and eventually completes the delivery
    Given the destination of delivery "DLV-100" is at position "44.55, 11.40"
    When drone "DRN-1" updates its position to "44.55, 11.40" with status "ARRIVED"
    Then the update should be applied
    And drone "DRN-1" should be in status "ARRIVED"
    And a "Drone Arrived" event should be published for drone "DRN-1"
    And delivery "DLV-100" should eventually be in status "DELIVERED"
    And the estimated time remaining for delivery "DLV-100" should eventually be "0"
    And drone "DRN-1" should eventually become available again

  Scenario: A drone going out of service mid-flight abolishes its delivery
    When drone "DRN-1" updates its status to "OUT_OF_SERVICE"
    Then the update should be applied
    And drone "DRN-1" should be in status "OUT_OF_SERVICE"
    And a "Drone Out Of Service" event should be published for drone "DRN-1"
    And delivery "DLV-100" should eventually be in status "ABOLISHED"
    And the drone reservation for delivery "DLV-100" should eventually be released

  Scenario: An idle drone updates fleet state without affecting any delivery
    Given drone "DRN-2" is a known drone with no assigned delivery
    When drone "DRN-2" updates its position to "44.49, 11.34" with status "AVAILABLE"
    Then the update should be applied
    And the current position of drone "DRN-2" should be "44.49, 11.34"
    And no delivery state should be changed
