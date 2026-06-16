Feature: Clinic Controller integration
  Covers clinic CRUD and clinic user management endpoints.

  Scenario: Create clinic as super admin
    Given clinic api request is authenticated as role "SUPER_ADMIN"
    And clinic api has a valid create payload
    When clinic api creates a clinic
    Then clinic response status should be 201
    And clinic response contains an id

  Scenario: Get clinic by id
    Given clinic api has an existing clinic
    When clinic api fetches clinic by id as role "ADMIN"
    Then clinic response status should be 200
    And clinic response id matches stored clinic id

  Scenario: List clinics paginated
    Given clinic api has an existing clinic
    When clinic api lists clinics as role "ADMIN"
    Then clinic response status should be 200
    And clinic page contains at least one clinic

  Scenario: List active clinics
    Given clinic api has an existing clinic
    When clinic api lists active clinics as role "USER"
    Then clinic response status should be 200
    And clinic active list is not empty

  Scenario: Update clinic
    Given clinic api has an existing clinic
    When clinic api updates that clinic as role "ADMIN"
    Then clinic response status should be 200
    And clinic name is updated

  Scenario: Deactivate clinic as super admin
    Given clinic api has an existing clinic
    When clinic api deactivates that clinic as role "SUPER_ADMIN"
    Then clinic response status should be 200

  Scenario: Create clinic user
    Given clinic api has an existing clinic
    When clinic api creates a clinic user as role "ADMIN"
    Then clinic response status should be 201
    And clinic user response contains an id

  Scenario: List clinic users
    Given clinic api has an existing clinic user
    When clinic api lists clinic users as role "ADMIN"
    Then clinic response status should be 200
    And clinic users list is not empty

  Scenario: Deactivate clinic user
    Given clinic api has an existing clinic user
    When clinic api deactivates clinic user as role "ADMIN"
    Then clinic response status should be 200

  Scenario: ADMIN cannot manage users of another clinic
    Given clinic api has two clinics with an admin for the first
    When that admin tries to list users of the second clinic
    Then clinic response status should be 403

