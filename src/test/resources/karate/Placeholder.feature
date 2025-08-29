Feature: rates-rm-rates-update-service-Placeholder

  Background:
    * url baseUrl

  Scenario: actuator available
    Given path '/actuator'
    When method get
    Then status 200