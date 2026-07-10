## ADDED Requirements

### Requirement: Browser Basic Auth Interception
When basic auth credentials are configured in the playbook, the browser target executor MUST intercept OS-native credential prompts at the driver protocol level using Selenium 4's `HasAuthentication` interface.

#### Scenario: Intercept basic auth prompt
- **WHEN** the browser navigates to a domain requiring basic authentication
- **THEN** the driver intercepts the challenge and supplies the credentials automatically, rendering the page without blocking.

### Requirement: API Header Authentication Injection
When API authorization is configured in the playbook, the REST executor MUST register request interceptors on the HTTP Client to automatically inject authentication headers on outbound requests.

#### Scenario: Intercept API request headers
- **WHEN** the REST client initiates an outbound request
- **THEN** the request interceptor automatically appends the Bearer token or Basic Auth header.
