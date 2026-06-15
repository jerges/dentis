Feature: Multi-tenant data isolation
  Verifies that clinic-scoped users can only access their own clinic's data,
  and that SUPER_ADMIN can access all data regardless of clinic.

  Scenario: ADMIN of clinic A only sees its own patients in the list
    Given two clinics A and B each have one patient
    When clinic A admin lists patients
    Then tenant response status should be 200
    And patient list contains exactly 1 patient

  Scenario: ADMIN of clinic A cannot fetch a patient from clinic B
    Given two clinics A and B each have one patient
    When clinic A admin fetches the patient from clinic B
    Then tenant response status should be 404

  Scenario: ADMIN of clinic A cannot fetch clinical record of patient from clinic B
    Given two clinics A and B each have one patient
    When clinic A admin fetches the clinical record of the patient from clinic B
    Then tenant response status should be 404

  Scenario: SUPER_ADMIN can access patients from any clinic
    Given two clinics A and B each have one patient
    When super admin fetches the patient from clinic B
    Then tenant response status should be 200

  Scenario: Creating a patient as clinic admin assigns it to that clinic
    Given a clinic exists for tenant isolation test
    When clinic admin creates a patient via the api
    Then tenant response status should be 201
    And the created patient belongs to the clinic
