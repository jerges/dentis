Feature: Dev auth controller integration
  Validates login behavior in dev profile.

  Scenario: Login with valid credentials
    When dev auth api logs in with username "jbello" and password "Admin@2026!"
    Then dev auth response status should be 200
    And dev auth response contains a token

  Scenario: Login with unknown user
    When dev auth api logs in with username "ghost-user" and password "Admin@2026!"
    Then dev auth response status should be 401

  Scenario: Login with wrong password
    When dev auth api logs in with username "jbello" and password "WrongPassword123!"
    Then dev auth response status should be 401

  Scenario: Login with incomplete payload
    When dev auth api logs in with incomplete payload
    Then dev auth response status should be 400

