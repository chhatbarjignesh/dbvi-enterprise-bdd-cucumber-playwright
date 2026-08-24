Feature: MMS Member Portal Login

  @MMS_REGRESSION @SMOKE @tmsLink=MMS-LOGIN-01 @severity=critical
  Scenario: Verify a registered member can log in successfully to the MMS Member Portal
    Given I navigate to the environment home page
    When I click on the login link
    And I login to MMS portal as "member" user
    Then I should be logged in successfully to MMS
