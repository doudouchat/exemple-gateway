Feature: gateway authorization token

  Background:
    Given get access token by authorize

  Scenario: api post by authorization code
    When perform post with JSESSIONID
    Then response status is 201

  Scenario: api get by authorization code
    When perform get with JSESSIONID
    Then response status is 200

  Scenario: api patch by authorization code
    When perform patch with JSESSIONID
    Then response status is 204

  Scenario: api head by authorization code
    When perform head with JSESSIONID
    Then response status is 204

  Scenario: api delete by authorization code
    When perform delete with JSESSIONID
    Then response status is 204

  Scenario: api options by authorization code
    When perform options with JSESSIONID
    Then response status is 200

