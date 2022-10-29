Feature: gateway direct

  Background: 
    Given create access token

  Scenario: api post direct
    When perform post with Authorization
    Then response status is 201

  Scenario: api get direct
    When perform get with Authorization
    Then response status is 200

  Scenario: api patch direct
    When perform patch with Authorization
    Then response status is 204

  Scenario: api head direct
    When perform head with Authorization
    Then response status is 204

  Scenario: api delete direct
    When perform delete with Authorization
    Then response status is 204

  Scenario: api options direct
    When perform options with Authorization
    Then response status is 200

