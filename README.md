# Async Processing Framework

A distributed asynchronous request processing framework optimized for AWS services.

## Overview

The Async Processing Framework provides a robust, AWS-native solution for handling long-running operations with support for:

- **Distributed Processing**: Handle async operations across multiple instances
- **AWS Native**: Optimized for AWS services (DynamoDB, SQS, S3)
- **State Management**: Track multi-step API call completion using DynamoDB
- **Storage Integration**: Support both inline and S3-stored large payloads
- **Retry Mechanisms**: Built-in SQS polling and retry with exponential backoff
- **Monitoring**: Comprehensive CloudWatch metrics and health checks
- **Spring Integration**: Auto-configuration and seamless Spring Boot integration

## Quick Start

### 1. Add Dependency

Add the framework dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.pravah.framework</groupId>
    <artifactId>async-processing-framework</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure Properties

Add configuration to your `application.properties`:

```properties
# AWS Configuration
async.framework.aws.region=ap-south-1
async.framework.aws.dynamodb.table-name=async-requests
async.framework.aws.s3.bucket-name=async-payloads

# SQS Queue URLs
async.framework.aws.sqs.queue-urls.default=https://sqs.ap-south-1.amazonaws.com/123456789012/async-requests
async.framework.aws.sqs.queue-urls.retry=https://sqs.ap-south-1.amazonaws.com/123456789012/async-requests-retry
```

### 3. Create Async Request

```java
@Component
public class MyAsyncRequest extends AsyncRequest {
    
    @Override
    protected CompletableFuture<Boolean> executeAsync() {
        // Your async processing logic here
        return CompletableFuture.completedFuture(true);
    }
    
    @Override
    public List<AsyncRequestAPI> getRequiredAPIs() {
        return List.of(AsyncRequestAPI.EXTERNAL_API_CALL);
    }
}
```

### 4. Use the Framework

```java
@Service
public class MyService {
    
    @Autowired
    private AsyncRequestBuilder asyncRequestBuilder;
    
    public void processAsync(String payload) {
        AsyncRequest request = asyncRequestBuilder
            .withType(AsyncRequestType.CUSTOM)
            .withAppId("my-app-123")
            .withPayload(payload)
            .build();
            
        request.process();
    }
}
```

## Architecture

The framework follows a layered architecture:

```
┌─────────────────────────────────────────┐
│           Client Applications           │
├─────────────────────────────────────────┤
│              Framework API              │
├─────────────────────────────────────────┤
│            Core Framework               │
├─────────────────────────────────────────┤
│           AWS Abstraction               │
├─────────────────────────────────────────┤
│            AWS Services                 │
└─────────────────────────────────────────┘
```

## Configuration

See the [Configuration Guide](docs/configuration.md) for detailed configuration options.

## Development

### Building

```bash
mvn clean compile
```

### Testing

```bash
mvn test
```

### Integration Testing

```bash
mvn verify
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.