package it.pagopa.pn.pecmock.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mock.sign")
public record SignServiceConfigurationProperties(Integer semaphores, Integer minDelay, Integer maxDelay,
                                                 Integer signDuration, Integer timestampingDuration,
                                                 Integer hashingDuration) {
}
