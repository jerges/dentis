Feature: Clinical Record Controller integration
  Covers clinical history, odontogram, evolutions, diagnoses and treatment plans.

  Scenario: Get or create clinical record for a patient
    Given clinical api has an existing patient
    When clinical api fetches or creates the clinical record
    Then clinical response status should be 200
    And clinical response contains a patient id

  Scenario: Get or create is idempotent
    Given clinical api has an existing patient
    When clinical api fetches or creates the clinical record
    And clinical api fetches or creates the clinical record again
    Then clinical response status should be 200

  Scenario: Update odontogram with tooth conditions
    Given clinical api has an existing patient
    When clinical api updates the odontogram with tooth conditions
    Then clinical response status should be 200
    And clinical odontogram contains updated teeth

  Scenario: Update odontogram with absent tooth and space status
    Given clinical api has an existing patient
    When clinical api marks a tooth as absent with space status OPEN
    Then clinical response status should be 200
    And clinical response contains a tooth with space status OPEN

  Scenario: Update dentition type to PRIMARY
    Given clinical api has an existing patient
    When clinical api updates dentition type to "PRIMARY"
    Then clinical response status should be 200
    And clinical response dentition type is "PRIMARY"

  Scenario: Update dentition type to MIXED
    Given clinical api has an existing patient
    When clinical api updates dentition type to "MIXED"
    Then clinical response status should be 200
    And clinical response dentition type is "MIXED"

  Scenario: Add clinical evolution
    Given clinical api has an existing patient
    When clinical api adds a valid evolution
    Then clinical response status should be 201
    And clinical response contains at least one evolution

  Scenario: Add evolution with blank description returns 400
    Given clinical api has an existing patient
    When clinical api adds an evolution with blank description
    Then clinical response status should be 400

  Scenario: Add diagnosis
    Given clinical api has an existing patient
    When clinical api adds a valid diagnosis
    Then clinical response status should be 201
    And clinical response contains at least one diagnosis

  Scenario: Add diagnosis without code returns 400
    Given clinical api has an existing patient
    When clinical api adds a diagnosis without code
    Then clinical response status should be 400

  Scenario: Add treatment plan
    Given clinical api has an existing patient
    When clinical api adds a valid treatment plan
    Then clinical response status should be 201
    And clinical response treatment plan status is "PROPOSED"

  Scenario: Clinical record for unknown patient returns 404
    When clinical api fetches clinical record for unknown patient
    Then clinical response status should be 404
