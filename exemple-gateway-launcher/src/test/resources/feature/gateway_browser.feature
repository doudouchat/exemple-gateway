Feature: gateway by browser

  Background:
    Given get csrf token
    And clear JSESSIONID

  Scenario: api post by browser
    Given login
    When perform post by browser
		Then response status is 201

  Scenario: api get by browser
    Given login
    When perform get by browser
    Then response status is 302
    When authorize by browser
		And token by browser
		When perform get by browser
		Then response status is 200

  Scenario: api patch by browser
    Given login
    When perform patch by browser
    Then response status is 302
    When authorize by browser
		And token by browser
		When perform patch by browser
		Then response status is 204

  Scenario: api head by browse
    When perform head by browser
    Then response status is 204

  Scenario: api delete by browser
    Given login
    When perform delete by browser
    Then response status is 302
    When authorize by browser
		And token by browser
		When perform delete by browser
		Then response status is 204

  Scenario: api options by browser
    When perform options by browser
    Then response status is 200

  Scenario: failure when bad xrsf token by browser
    Given use XSRF-TOKEN 'bad xrsf token'
    When perform post by browser
    Then response status is 403

