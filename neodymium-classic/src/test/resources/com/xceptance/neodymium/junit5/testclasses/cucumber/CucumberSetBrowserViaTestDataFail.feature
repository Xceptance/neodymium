Feature: Set browser via test data fail

  Scenario Outline: Set browsers
    Given "<browser>" is open
     Then the browser "<browser>" is setup

    Examples: 
      | browser          |
      | Chrome_Not_Found |
