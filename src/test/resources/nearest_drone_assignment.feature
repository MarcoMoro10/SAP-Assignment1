# Acceptance test of the delivery-service (Fleet is an internal module, not a
# separate service — see architecture §3). These scenarios exercise the
# delivery-service's public REST API in the When/Then, while the Given configures
# the initial state of the internal Fleet module IN-PROCESS, through the domain
# (drones are created via their legitimate factory/constructor and registered in
# the in-memory fleet repository, respecting domain invariants). There is NO REST
# endpoint to set fleet state: the fleet has no network surface of its own (§7.1),
# so the Given works against the domain directly. This is expected and is declared
# here so that reading the .feature does not suggest a "set fleet state" API exists.

Feature: Nearest drone assignment
  As a Sender,
  I want the nearest available drone to be assigned to my immediate delivery
  so that my package is picked up as quickly as possible.

  Background:
    Given I am logged in as "user-1" with password "Secret#123"
    And I am on the delivery creation page

  Scenario: The nearest eligible drone is assigned
    Given drone "DRN-1" is available "2" km from the pickup point
    And drone "DRN-2" is available "5" km from the pickup point
    And both drones can carry the package
    When I create a delivery with weight "2" kg, starting place "via Emilia, 9", destination place "via Veneto, 5" to ship immediately
    Then drone "DRN-1" should be assigned to the delivery
    And the delivery should be in status "ASSIGNED"

  Scenario: The nearest drone is skipped when it cannot carry the package
    Given drone "DRN-1" is available "2" km from the pickup point with max capacity "1" kg
    And drone "DRN-2" is available "5" km from the pickup point with max capacity "5" kg
    When I create a delivery with weight "3" kg, starting place "via Emilia, 9", destination place "via Veneto, 5" to ship immediately
    Then drone "DRN-2" should be assigned to the delivery
    And the delivery should be in status "ASSIGNED"
