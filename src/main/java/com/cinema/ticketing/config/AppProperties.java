package com.cinema.ticketing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final RateLimit rateLimit = new RateLimit();

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public static class RateLimit {
        private int requestsPerMinute = 100;

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }
    }
}
