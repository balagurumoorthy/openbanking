Feature: Account information consent and access

  Background:
    Given the Open Banking platform is reachable

  Scenario: User grants scoped consent and TPP reads account data
    Given MohanaTPP initiates an account-access consent for "ReadAccountsDetail,ReadBalances,ReadTransactionsDetail"
    And customer "alice" logs in and approves account "GB-ALICE-001"
    When MohanaTPP exchanges the authorization code for an access token
    And MohanaTPP requests accounts via the gateway
    Then the response status is 200
    And the accounts response contains account "GB-ALICE-001"

  Scenario: Access without a token is rejected by the gateway
    When an unauthenticated request is made to the accounts endpoint
    Then the response status is 401

  Scenario: User denies consent
    Given MohanaTPP initiates an account-access consent for "ReadAccountsDetail"
    When customer "alice" denies the consent
    Then the redirect contains error "access_denied"
