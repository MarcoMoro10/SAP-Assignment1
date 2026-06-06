Feature: Telemetry ingestion
  As the Shipping on the Air system,
  I want to ingest position and status updates emitted by the drones
  so that delivery tracking and fleet monitoring reflect the real state of the fleet.

  # Telemetry is received by the Fleet Context (U/OHS -> F/ACL).
  # The Fleet Context updates its own drone state and publishes domain events
  # (e.g. Position Updated, Drone Arrived) on the async event stream.
  # The Deliveries Context consumes those events through its ACL and updates
  # the delivery / tracking view accordingly. Cross-context effects are therefore
  # expressed as eventual consequences, not atomic side-effects.

  Background:
    Given drone "DRN-1" is a known drone registered in the fleet
    And drone "DRN-1" is assigned to delivery "DLV-100" in status "IN_PROGRESS"

  Scenario: A valid telemetry update is accepted and stored by the Fleet Context
    When drone "DRN-1" reports position "44.50, 11.35" with status "IN_DELIVERY"
    Then the update should be accepted
    And the current position of drone "DRN-1" should be "44.50, 11.35"
    And a "Position Updated" event should be published for drone "DRN-1"

  Scenario: A valid position update is eventually reflected in delivery tracking
    When drone "DRN-1" reports position "44.50, 11.35" with status "IN_DELIVERY"
    Then the update should be accepted
    And the tracking view of delivery "DLV-100" should eventually show position "44.50, 11.35"

  Scenario: Telemetry for an unknown drone is rejected
    When an unknown drone "DRN-999" reports position "44.50, 11.35" with status "IN_DELIVERY"
    Then the update should be rejected with the error "Unknown drone"
    And no delivery state should be changed

  Scenario: Telemetry with an invalid position is rejected
    When drone "DRN-1" reports position "999.0, 999.0" with status "IN_DELIVERY"
    Then the update should be rejected with the error "Invalid position"
    And the last known position of drone "DRN-1" should not change

  Scenario: Arrival telemetry marks the drone as arrived and eventually completes the delivery
    Given the destination of delivery "DLV-100" is at position "44.55, 11.40"
    When drone "DRN-1" reports position "44.55, 11.40" with status "ARRIVED"
    Then the update should be accepted
    And drone "DRN-1" should be in status "ARRIVED"
    And a "Drone Arrived" event should be published for drone "DRN-1"
    And delivery "DLV-100" should eventually be in status "DELIVERED"
    And the estimated time remaining for delivery "DLV-100" should eventually be "0"
    And drone "DRN-1" should eventually become available again

  Scenario: A drone going out of service mid-flight abolishes its delivery
    When drone "DRN-1" reports status "OUT_OF_SERVICE"
    Then the update should be accepted
    And drone "DRN-1" should be in status "OUT_OF_SERVICE"
    And a "Drone Out Of Service" event should be published for drone "DRN-1"
    And delivery "DLV-100" should eventually be in status "ABOLISHED"
    And the drone reservation for delivery "DLV-100" should eventually be released

  Scenario: Telemetry for an idle drone updates fleet state without affecting any delivery
    Given drone "DRN-2" is a known drone with no assigned delivery
    When drone "DRN-2" reports position "44.49, 11.34" with status "AVAILABLE"
    Then the update should be accepted
    And the current position of drone "DRN-2" should be "44.49, 11.34"
    And no delivery state should be changed
