@SetUpWithBrowserTag
Feature: Set browser via tag

	@Chrome_headless @FF_1024x768
  Scenario: Set browsers
    Given the browser "Chrome_headless" is setup

