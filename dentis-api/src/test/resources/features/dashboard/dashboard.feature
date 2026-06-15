Feature: Dashboard Controller integration
  Covers aggregated statistics endpoint.

  Scenario: Stats endpoint returns expected fields
    When dashboard api fetches stats
    Then dashboard response status should be 200
    And dashboard response contains stats fields

  Scenario: Stats reflect seeded patients and payments
    Given dashboard has 3 patients and 1 payment of 500.00
    When dashboard api fetches stats
    Then dashboard response status should be 200
    And dashboard total patients is at least 3
    And dashboard total revenue is at least 500.00

  Scenario: Stats return zero revenue when no payments exist
    When dashboard api fetches stats
    Then dashboard response status should be 200
    And dashboard total revenue is 0.00
