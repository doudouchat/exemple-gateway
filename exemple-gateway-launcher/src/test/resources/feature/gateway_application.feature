Feature: gateway by application

  Background:
    Given clear JSESSIONID

  Scenario: api post by application
    When perform post by application
    Then response status is 201

  Scenario: api get by application
    Given login
    When perform get by application
    Then response status is 302
    When authorize by application
		And token by application
		When perform get by application
		Then response status is 200

  Scenario: api patch by application
    Given login
    When perform patch by application
    Then response status is 302
    When authorize by application
		And token by application
		When perform patch by application
		Then response status is 204

  Scenario: api head by application
    When perform head by application
    Then response status is 204

  Scenario: api delete by application
    Given login
    When perform delete by application
    Then response status is 302
    When authorize by application
		And token by application
		When perform delete by application
		Then response status is 204

  Scenario: api options by application
    When perform options by application
    Then response status is 200

