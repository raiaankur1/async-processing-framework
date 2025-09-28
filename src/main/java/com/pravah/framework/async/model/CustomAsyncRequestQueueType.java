package com.pravah.framework.async.model;

import java.time.Duration;

public class CustomAsyncRequestQueueType {
    private final String queueName;
    private final int delaySeconds;
    private final String description;

    public CustomAsyncRequestQueueType(String queueName, int delaySeconds, String description) {
        this.queueName = queueName;
        this.delaySeconds = delaySeconds;
        this.description = description;
    }

    public String getQueueName() { return queueName; }
    public int getDelaySeconds() { return delaySeconds; }
    public Duration getDelay() { return Duration.ofSeconds(delaySeconds); }
    public String getDescription() { return description; }
    public String getParamName() { return String.valueOf(delaySeconds); }
    public boolean isCustomQueue() { return true; }

    @Override
    public String toString() { return this.queueName; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CustomAsyncRequestQueueType that = (CustomAsyncRequestQueueType) obj;
        return queueName.equals(that.queueName);
    }

    @Override
    public int hashCode() { return queueName.hashCode(); }
}