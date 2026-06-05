Feature: Telemetry ingestion
  As the Shipping on the Air system,
  I want to ingest position and status updates emitted by the drones  
  so that delivery tracking and fleet monitoring reflect the real state of the fleet.

  Background:
    Given drone "DRN-1" is a known drone registered in the fleet
    And drone "DRN-1" is assigned to delivery "DLV-100" in status "IN_PROGRESS"

  Scenario: A valid telemetry update is accepted and stored
    When drone "DRN-1" reports position "44.50, 11.35" with status "IN_DELIVERY"
    Then the update should be accepted
    And the current position of drone "DRN-1" should be "44.50, 11.35"
    And the current position of delivery "DLV-100" should be "44.50, 11.35"

  Scenario: Telemetry for an unknown drone is rejected
    When an unknown drone "DRN-999" reports position "44.50, 11.35" with status "IN_DELIVERY"
    Then the update should be rejected with the error "Unknown drone"
    And no delivery state should be changed

  Scenario: Telemetry with an invalid position is rejected
    When drone "DRN-1" reports position "999.0, 999.0" with status "IN_DELIVERY"
    Then the update should be rejected with the error "Invalid position"
    And the last known position of drone "DRN-1" should not change

  Scenario: Arrival telemetry marks the delivery as completed
    Given the destination of delivery "DLV-100" is at position "44.55, 11.40"
    When drone "DRN-1" reports position "44.55, 11.40" with status "ARRIVED"
    Then the update should be accepted
    And delivery "DLV-100" should be in status "DELIVERED"
    And the estimated time remaining for delivery "DLV-100" should be "0"
    And drone "DRN-1" should become available again

  Scenario: Telemetry for an idle drone updates fleet state without affecting any delivery
    Given drone "DRN-2" is a known drone with no assigned delivery
    When drone "DRN-2" reports position "44.49, 11.34" with status "AVAILABLE"
    Then the update should be accepted
    And the current position of drone "DRN-2" should be "44.49, 11.34"
    And no delivery state should be changed
