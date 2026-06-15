Feature: Patient Controller integration
  Covers CRUD and search flows for patient endpoints.

  Scenario: Create patient successfully
    Given patient api has a valid create payload
    When patient api creates a patient
    Then patient response status should be 201
    And patient response contains an id

  Scenario: Get patient by id
    Given patient api has an existing patient
    When patient api fetches that patient by id
    Then patient response status should be 200
    And patient response id matches stored patient id

  Scenario: List patients returns at least one record
    Given patient api has an existing patient
    When patient api lists patients
    Then patient response status should be 200
    And patient list contains at least one patient

  Scenario: Search patients by name
    Given patient api has an existing patient
    When patient api searches patients by name
    Then patient response status should be 200
    And patient search result is not empty

  Scenario: Update patient basic data
    Given patient api has an existing patient
    When patient api updates that patient
    Then patient response status should be 200
    And patient first name is updated

  Scenario: Deactivate patient
    Given patient api has an existing patient
    When patient api deactivates that patient
    Then patient deactivate status should be 204

  Scenario: Duplicate id document returns business error
    Given patient api has an existing patient
    When patient api tries to create another patient with same id document
    Then patient response status should be 422

  Scenario: Unknown patient id returns 404
    When patient api fetches an unknown patient
    Then patient response status should be 404

  Scenario: Update non-existent patient returns 404
    When patient api updates a non-existent patient
    Then patient response status should be 404

