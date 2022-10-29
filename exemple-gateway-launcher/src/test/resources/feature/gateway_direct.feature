Feature: gateway direct

  Background: 
    Given create access token

  Scenario: api post
    When perform post with Authorization
    Then response status is 201

  Scenario: api get
    When perform get with Authorization
    Then response status is 200

  Scenario: api patch
    When perform patch with Authorization
    Then response status is 204

  Scenario: api head
    When perform head with Authorization
    Then response status is 204

  Scenario: api delete
    When perform delete with Authorization
    Then response status is 204

  Scenario: api options
    When perform options with Authorization
    Then response status is 200

