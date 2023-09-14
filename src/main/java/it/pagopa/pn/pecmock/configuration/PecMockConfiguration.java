package it.pagopa.pn.pecmock.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mock.pec")
public record PecMockConfiguration(Integer semaphores, Integer minDelay, Integer maxDelay) {}