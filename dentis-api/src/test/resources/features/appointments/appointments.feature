Feature: Appointment Controller integration
  Covers scheduling, status transitions and query endpoints.

  Scenario: Schedule appointment successfully
    Given appointment api has an existing patient and dentist
    When appointment api schedules a valid appointment
    Then appointment response status should be 201
    And appointment response status field is "SCHEDULED"

  Scenario: Get appointment by id
    Given appointment api has an existing scheduled appointment
    When appointment api fetches appointment by id
    Then appointment response status should be 200
    And appointment response id matches stored appointment id

  Scenario: Confirm scheduled appointment
    Given appointment api has an existing scheduled appointment
    When appointment api confirms appointment
    Then appointment response status should be 200
    And appointment response status field is "CONFIRMED"

  Scenario: Cancel appointment
    Given appointment api has an existing scheduled appointment
    When appointment api cancels appointment
    Then appointment cancel status should be 204

  Scenario: Reschedule appointment
    Given appointment api has an existing scheduled appointment
    When appointment api reschedules appointment to another valid slot
    Then appointment response status should be 200
    And appointment response start date is updated

  Scenario: List appointments by patient
    Given appointment api has an existing scheduled appointment
    When appointment api lists appointments by patient
    Then appointment response status should be 200
    And appointment list contains at least one record

  Scenario: List appointments by dentist and date range
    Given appointment api has an existing scheduled appointment
    When appointment api lists appointments by dentist and date range
    Then appointment response status should be 200
    And appointment list contains at least one record

  Scenario: Scheduling conflict returns business error
    Given appointment api has an existing scheduled appointment
    When appointment api schedules another appointment in conflicting slot
    Then appointment response status should be 422

  Scenario: Invalid duration returns business error
    Given appointment api has an existing patient and dentist
    When appointment api schedules appointment with end before start
    Then appointment response status should be 422

  Scenario: Confirm non-existent appointment returns 404
    When appointment api confirms a non-existent appointment
    Then appointment response status should be 404

  Scenario: Cancel non-existent appointment returns 404
    When appointment api cancels a non-existent appointment
    Then appointment cancel status should be 404

