Feature: Cancel a delivery request
  As a logged-in Sender,
  I want to cancel a delivery request
  so that I can stop a shipment I no longer need.

  Background:
    Given I am logged in as "user-1" with password "Secret#123"

  Scenario: Cancel a delivery that has not started yet
    Given I have a delivery "DLV-100" in status "Assigned"
    When I cancel the delivery "DLV-100"
    Then I should see a confirmation that the delivery has been cancelled
    And the assigned drone should become available again

  Scenario: Cannot cancel a delivery already in flight
    Given I have a delivery "DLV-100" in status "InProgress"
    When I cancel the delivery "DLV-100"
    Then I should see the error "Delivery cannot be cancelled once in flight"
    And the delivery should remain in status "InProgress"

  Scenario: Cannot cancel a delivery that belongs to another user
    Given a delivery "DLV-200" belongs to another user
    When I cancel the delivery "DLV-200"
    Then I should see the error "Delivery not found"
