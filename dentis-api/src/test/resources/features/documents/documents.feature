Feature: Document Controller integration
  Covers folder management, document registration, search, and access control.

  Scenario: Initialize knowledge-base root folder
    When documents api initializes the knowledge-base root as ADMIN
    Then documents response status should be 201
    And documents response contains a folder named "Base de Conocimiento IA"

  Scenario: Initialize knowledge-base is idempotent
    Given documents api has a clinic with knowledge-base initialized
    When documents api initializes the knowledge-base root as ADMIN
    Then documents response status should be 201
    And documents response contains a folder named "Base de Conocimiento IA"

  Scenario: Create GENERAL folder as ADMIN
    When documents api creates a GENERAL folder named "Facturas"
    Then documents response status should be 201
    And documents response contains a folder named "Facturas"

  Scenario: Create KNOWLEDGE_BASE folder as ADMIN
    When documents api creates a KNOWLEDGE_BASE folder named "Protocolos Clínicos"
    Then documents response status should be 201
    And documents response contains a folder named "Protocolos Clínicos"

  Scenario: USER is rejected when creating KNOWLEDGE_BASE folder
    When documents api user tries to create a KNOWLEDGE_BASE folder named "Secretos"
    Then documents response status should be 400

  Scenario: List root folders returns created folders
    Given documents api has a GENERAL folder named "Facturas"
    When documents api lists root folders as ADMIN
    Then documents response status should be 200
    And documents response folder list is not empty

  Scenario: Delete non-system GENERAL folder
    Given documents api has a GENERAL folder named "Para Borrar"
    When documents api deletes that folder as ADMIN
    Then documents response status should be 204

  Scenario: Delete system knowledge-base folder is rejected with 400
    Given documents api has a clinic with knowledge-base initialized
    When documents api deletes the system knowledge-base folder as ADMIN
    Then documents response status should be 400

  Scenario: Register a document in a GENERAL folder
    Given documents api has a GENERAL folder named "Facturas Registradas"
    When documents api registers a document "factura.pdf" in that folder
    Then documents response status should be 201
    And documents document response has fileName "factura.pdf"

  Scenario: List documents in a folder
    Given documents api has a GENERAL folder named "Carpeta Listado"
    And documents api has a document "protocolo.pdf" in that folder
    When documents api lists documents in that folder
    Then documents response status should be 200
    And documents response document list is not empty

  Scenario: Full-text search returns results
    Given documents api has a GENERAL folder named "Carpeta Busqueda"
    And documents api has a document "manual-procedimiento.pdf" in that folder
    When documents api searches for "manual"
    Then documents response status should be 200
