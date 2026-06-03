Feature: Delivery scheduling management
  As an Admin,
  I want to manage the scheduling of delivery requests for each drone
  so that the daily route of every drone is organised efficiently.

  Background:
    Given I am logged in as admin "admin-1" with password "Admin#123"

  Scenario: View the daily schedule of a drone
    Given drone "DRN-1" has "2" scheduled deliveries today
    When I open the scheduling page for drone "DRN-1"
    Then I should see "2" scheduled deliveries
    And they should be ordered by their pickup time

  Scenario: A scheduled delivery reserves a drone for the requested slot
    Given a scheduled delivery "DLV-300" is planned for "2026-06-10" at "10:00"
    When the scheduler assigns it to drone "DRN-1"
    Then drone "DRN-1" should be reserved for "2026-06-10" at "10:00"
    And drone "DRN-1" should not be assignable to another delivery in that slot

  Scenario: Scheduling fails when no drone is free for the requested slot
    Given all drones are already reserved for "2026-06-10" at "10:00"
    When a scheduled delivery is requested for "2026-06-10" at "10:00"
    Then the request should be rejected with the error "No drone available for the requested time"
