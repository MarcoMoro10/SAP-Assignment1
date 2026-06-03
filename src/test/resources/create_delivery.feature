Feature: Create a delivery request
  As a logged-in Sender,
  I want to create a delivery request specifying package and addresses
  so that I can ship a package from one place to another.

  Background:
    Given I am logged in as "user-1" with password "Secret#123"
    And I am on the delivery creation page
    And deliveries can be scheduled at most "7" days in advance

  Scenario: Successful immediate delivery creation
    When I create a delivery with weight "2" kg, starting place "via Emilia, 9", destination place "via Veneto, 5" to ship immediately
    Then I should see a confirmation that the delivery has been created and receive its identifier
    And a drone should be assigned to the delivery

  Scenario: Successful scheduled delivery creation
    When I create a delivery with weight "2" kg, starting place "via Emilia, 9", destination place "via Veneto, 5" to ship on "2026-06-10" at "10:00"
    Then I should see a confirmation that the delivery has been created and receive its identifier
    And a drone should be reserved for "2026-06-10" at "10:00"

  Scenario: Delivery creation fails when the shipping time exceeds the scheduling horizon
    When I create a delivery with weight "2" kg, starting place "via Emilia, 9", destination place "via Veneto, 5" to ship in "30" days
    Then I should see the error "Shipping time exceeds the maximum scheduling horizon"
    And the delivery should not be created

  Scenario: Delivery creation fails with a shipping time in the past
    When I create a delivery with weight "2" kg, starting place "via Emilia, 9", destination place "via Veneto, 5" to ship on "2020-01-01" at "10:00"
    Then I should see the error "Invalid shipping time"
    And the delivery should not be created

  Scenario: Delivery rejected because the package is too heavy for any drone
    Given the maximum load capacity in the fleet is "5" kg
    When I create a delivery with weight "8" kg, starting place "via Emilia, 9", destination place "via Veneto, 5" to ship immediately
    Then I should see the error "No drone can carry this package"
    And the delivery should not be created

  Scenario: Delivery rejected because it cannot meet the maximum delivery time
    When I create a delivery with weight "2" kg, starting place "via Emilia, 9", destination place "via Roma, 200" to ship immediately within "5" minutes
    Then I should see the error "Delivery cannot be completed within the requested time"
    And the delivery should not be created

  Scenario: Delivery rejected because no drone is available
    Given all drones in the fleet are currently busy
    When I create a delivery with weight "2" kg, starting place "via Emilia, 9", destination place "via Veneto, 5" to ship immediately
    Then I should see the error "No drone available"
    And the delivery should not be created

  Scenario: Delivery creation fails with an invalid address
    When I create a delivery with weight "2" kg, starting place "xxxxx", destination place "via Veneto, 5" to ship immediately
    Then I should see the error "Invalid address"
    And the delivery should not be created
