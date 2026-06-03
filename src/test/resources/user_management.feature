Feature: User management
  As an Admin,
  I want to manage users and their profiles
  so that I can administer who has access to the service.

  Background:
    Given I am logged in as admin "admin-1" with password "Admin#123"

  Scenario: View the list of users
    Given "3" users are registered
    When I open the user management page
    Then I should see "3" users

  Scenario: Disable a user account
    Given a user "user-1" exists and is active
    When I disable the user "user-1"
    Then the user "user-1" should be marked as disabled
    And user "user-1" should not be able to log in

  Scenario: Disabling a non-existing user fails
    When I disable the user "user-999"
    Then I should see the error "User not found"
