Feature: gateway browser

  Background: 
    Given get access token by client credentials

  Scenario: api post by cookie
    When perform post with JSESSIONID
    Then response status is 201

  Scenario: api get by cookie
    When perform get with JSESSIONID
    Then response status is 200
 
  Scenario: api patch by cookie
    When perform patch with JSESSIONID
    Then response status is 204

  Scenario: api head by cookie
    When perform head with JSESSIONID
    Then response status is 204

  Scenario: api delete by cookie
    When perform delete with JSESSIONID
    Then response status is 204

  Scenario: api options by cookie
    When perform options with JSESSIONID
    Then response status is 200

  Scenario: failure when bad session id by cookie
    Given use JSESSIONID 'bad jsession id'
    When perform post with JSESSIONID
    Then response status is 401

  Scenario: failure when bad xrsf token by cookie
    Given use XSRF-TOKEN 'bad xrsf token'
    When perform post with JSESSIONID
    Then response status is 403

