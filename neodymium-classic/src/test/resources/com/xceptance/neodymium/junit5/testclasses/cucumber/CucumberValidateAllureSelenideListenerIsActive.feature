@SetUpWithBrowserTag
Feature: Set browser via tag

	@Chrome_headless
  Scenario: Set browsers
    Given the browser "Chrome_headless" is setup
    Then validate the AllureSelenide listener is active

