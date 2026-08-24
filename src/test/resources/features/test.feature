Feature: Example Playwright Test

  @SMOKE @EXAMPLE_DOMAIN
  Scenario: Verify Example Domain Title
    Given I navigate to "https://example.com"
    Then I should see the text "Example Domain"

  @SMOKE @DYNAMIC_ENV
  Scenario: Verify Environment Dynamic Test Data
    Given I navigate to the mock home page
    And I login as "regularuser" user
    And I login as "rewarduser" user
    And I login as "diamonduser" user
    Then I should see the text "Example Domain"
    And I should verify the environment search properties
