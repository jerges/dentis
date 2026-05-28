Feature: Billing Controller - Budget and Payment CRUD
  Integration tests for the BillingController REST API endpoints.
  Covers happy paths and error cases for budgets and payments.

  Background:
    Given a tariff and a patient exist in the database

  # ─────────────────────────────────────────
  # BUDGET SCENARIOS
  # ─────────────────────────────────────────

  Scenario: Create a treatment budget with valid items
    When I create a budget with valid items
    Then the response status should be 201
    And the budget status in response should be "DRAFT"

  Scenario: Get an existing budget by ID
    Given a budget has been created
    When I get the budget by its ID
    Then the response status should be 200
    And the response contains the budget data

  Scenario: Get a budget that does not exist returns 404
    When I get budget with unknown ID
    Then the response status should be 404

  Scenario: Approve a DRAFT budget
    Given a budget has been created
    When I approve the budget
    Then the response status should be 200
    And the budget status in response should be "APPROVED"

  Scenario: Cannot approve a budget that is already approved
    Given an approved budget exists
    When I approve the budget
    Then the response status should be 422

  Scenario: Get the financial summary of a budget
    Given an approved budget exists
    When I get the budget summary
    Then the response status should be 200
    And the summary shows a pending payment status

  Scenario: List budgets for a patient
    Given a budget has been created
    When I list budgets for the patient
    Then the response status should be 200
    And the budget list is not empty

  # ─────────────────────────────────────────
  # PAYMENT SCENARIOS
  # ─────────────────────────────────────────

  Scenario: Register a payment for an approved budget
    Given an approved budget exists
    When I register a payment of 100.00 for the budget
    Then the response status should be 201
    And the payment amount in response is 100.00

  Scenario: Cannot register a payment for a DRAFT budget
    Given a budget has been created
    When I register a payment of 50.00 for the budget
    Then the response status should be 422

  Scenario: Cannot register a payment exceeding the remaining balance
    Given an approved budget exists
    When I register a payment exceeding the budget balance
    Then the response status should be 422

  Scenario: List payments for a budget
    Given an approved budget exists with a registered payment of 100.00
    When I list payments for the budget
    Then the response status should be 200
    And the payment list contains 1 item

  Scenario: List payments for a patient
    Given an approved budget exists with a registered payment of 100.00
    When I list payments for the patient
    Then the response status should be 200
    And the payment list contains 1 item
