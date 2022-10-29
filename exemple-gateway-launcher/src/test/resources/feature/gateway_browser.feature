Feature: gateway browser

  Background: 
    Given get access token by client credentials

  Scenario: api post
    When perform post with JSESSIONID
    Then response status is 201

  Scenario: api get
    When perform get with JSESSIONID
    Then response status is 200
 
  Scenario: api patch
    When perform patch with JSESSIONID
    Then response status is 204

  Scenario: api head
    When perform head with JSESSIONID
    Then response status is 204

  Scenario: api delete
    When perform delete with JSESSIONID
    Then response status is 204

  Scenario: api options
    When perform options with JSESSIONID
    Then response status is 200

  Scenario: failure when bad session id
    Given use JSESSIONID 'bad jsession id'
    When perform post with JSESSIONID
    Then response status is 401

  Scenario: failure when bad xrsf token
    Given use XSRF-TOKEN 'bad xrsf token'
    When perform post with JSESSIONID
    Then response status is 403

