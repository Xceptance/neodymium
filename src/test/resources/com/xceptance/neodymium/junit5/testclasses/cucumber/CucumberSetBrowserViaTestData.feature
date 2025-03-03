Feature: Set browser via test data

  Scenario Outline: Set browsers
    Given "<browser>" is open
     Then the browser "<browser>" is setup

    Examples: 
      | browser          |
      | Chrome_headless  |