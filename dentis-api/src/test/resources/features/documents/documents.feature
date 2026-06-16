Feature: Document management - folders, KB access control and multi-tenant isolation
  Integration tests for DocumentController REST endpoints.
  Covers folder CRUD, Knowledge Base protection, visibility rules and tenant isolation.

  Background:
    Given a clinic exists for documents tests

  # ─────────────────────────────────────────
  # FOLDER CRUD
  # ─────────────────────────────────────────

  Scenario: ADMIN creates a root folder
    When clinic admin creates a folder named "Protocolos"
    Then documents response status should be 201
    And the folder name in response is "Protocolos"

  Scenario: List root folders returns only accessible folders
    Given clinic admin creates a PUBLIC folder named "Publica"
    When clinic admin lists root folders
    Then documents response status should be 200
    And the folder list contains "Publica"

  # ─────────────────────────────────────────
  # KNOWLEDGE BASE
  # ─────────────────────────────────────────

  Scenario: Regular user cannot access the Knowledge Base endpoint
    When clinic user requests the knowledge base folder
    Then documents response status should be 403

  Scenario: ADMIN can access the Knowledge Base folder
    When clinic admin requests the knowledge base folder
    Then documents response status should be 200
    And the folder type in response is "KNOWLEDGE_BASE"

  Scenario: Knowledge Base folder cannot be deleted
    Given the knowledge base folder exists for the clinic
    When clinic admin deletes the knowledge base folder
    Then documents response status should be 400

  # ─────────────────────────────────────────
  # MULTI-TENANT ISOLATION
  # ─────────────────────────────────────────

  Scenario: ADMIN of clinic A cannot see folders from clinic B
    Given clinic B has a folder named "Secreto"
    When clinic A admin lists root folders
    Then documents response status should be 200
    And the folder list does not contain "Secreto"

  Scenario: SUPER_ADMIN can access folders from any clinic
    Given clinic B has a folder named "Secreto"
    When super admin lists folders for clinic B
    Then documents response status should be 200
    And the folder list contains "Secreto"

  # ─────────────────────────────────────────
  # VISIBILITY RULES
  # ─────────────────────────────────────────

  Scenario: PRIVATE folder is not visible to other users of same clinic
    Given clinic admin creates a PRIVATE folder named "MisCosas"
    When another user of same clinic lists root folders
    Then the folder list does not contain "MisCosas"

  Scenario: PUBLIC folder is visible to all users of the same clinic
    Given clinic admin creates a PUBLIC folder named "Compartida"
    When another user of same clinic lists root folders
    Then the folder list contains "Compartida"
