package it.pagopa.pn.pecmock.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mock.sign")
public record SignServiceProperties(Integer semaphores, Integer minDelay, Integer maxDelay) {
}
