# Async Framework Configuration

This package contains the configuration classes for the Async Processing Framework. These classes provide comprehensive configuration options for AWS services and framework behavior.

## Configuration Classes

### AsyncFrameworkConfig
The main configuration class that binds to Spring Boot properties with prefix `async.framework`.

**Key Properties:**
- `enabled`: Enable/disable the framework
- `aws`: AWS-specific configuration
- `retry`: Retry behavior configuration
- `monitoring`: Monitoring and observability settings
- `queues`: Queue-specific configurations
- `storage`: Storage behavior settings

### AWSConfig
AWS service configuration with support for credentials, regions, and service-specific settings.

**Key Properties:**
- `region`: AWS region (default: ap-south-1)
- `accessKey`/`secretKey`: Explicit credentials (optional)
- `roleArn`: IAM role for assumption (optional)
- `useDefaultCredentialsProvider`: Use AWS default credential chain
- Connection timeouts and retry settings

### DynamoDBConfig
DynamoDB-specific configuration for table management and performance.

**Key Properties:**
- `tableName`: Table name for async requests
- `billingMode`: PAY_PER_REQUEST or PROVISIONED
- `readCapacity`/`writeCapacity`: Capacity units for provisioned mode
- `enableTtl`: Enable automatic cleanup
- `globalSecondaryIndexes`: GSI configurations

### SQSConfig
SQS configuration for message queuing and processing.

**Key Properties:**
- `queueUrls`: Map of queue names to URLs
- `visibilityTimeoutSeconds`: Message visibility timeout
- `maxMessages`: Max messages per poll
- `enableLongPolling`: Enable long polling
- `maxConcurrentProcessors`: Concurrent message processors
- `enableBatchProcessing`: Enable batch operations

### S3Config
S3 configuration for payload storage and lifecycle management.

**Key Properties:**
- `bucketName`: S3 bucket for payloads
- `keyPrefix`: Object key prefix
- `storageClass`: S3 storage class
- `encryption`: Server-side encryption settings
- `lifecyclePolicy`: Automatic lifecycle transitions
- `multipartUpload`: Multipart upload configuration

## Usage Examples

### Basic Configuration (application.properties)
```properties
# Enable framework
async.framework.enabled=true

# AWS Configuration
async.framework.aws.region=ap-south-1
async.framework.aws.use-default-credentials-provider=true

# DynamoDB
async.framework.aws.dynamodb.table-name=async_requests
async.framework.aws.dynamodb.billing-mode=PAY_PER_REQUEST

# SQS
async.framework.aws.sqs.queue-urls.default=https://sqs.ap-south-1.amazonaws.com/123456789012/async-requests
async.framework.aws.sqs.enable-long-polling=true

# S3
async.framework.aws.s3.bucket-name=my-async-payloads
async.framework.aws.s3.encryption.enabled=true
```

### Programmatic Configuration
```java
@Configuration
public class AsyncFrameworkConfiguration {
    
    @Bean
    @ConfigurationProperties("async.framework")
    public AsyncFrameworkConfig asyncFrameworkConfig() {
        return new AsyncFrameworkConfig();
    }
}
```

### Environment-Specific Configurations

#### Development
```java
AWSConfig devConfig = AWSConfig.forLocalStack("http://localhost:4566");
DynamoDBConfig devDynamoConfig = DynamoDBConfig.forDevelopment();
S3Config devS3Config = S3Config.forDevelopment();
```

#### Production
```java
AWSConfig prodConfig = AWSConfig.forProduction("ap-south-1");
DynamoDBConfig prodDynamoConfig = DynamoDBConfig.forProduction();
S3Config prodS3Config = S3Config.forProduction();
```

## Validation

All configuration classes include comprehensive validation:

- **Range validation**: Numeric values within acceptable ranges
- **Format validation**: AWS resource names follow conventions
- **Dependency validation**: Related settings are consistent
- **Security validation**: Credential configurations are complete

## Factory Methods

Each configuration class provides factory methods for common scenarios:

- `forDevelopment()`: Optimized for local development
- `forProduction()`: Production-ready settings
- `forLocalStack()`: LocalStack integration
- `forHighThroughput()`: High-performance scenarios
- `forReliability()`: Maximum reliability settings

## Spring Boot Integration

The configuration classes are designed for seamless Spring Boot integration:

- `@ConfigurationProperties` binding
- `@Validated` annotation support
- IDE autocomplete via configuration metadata
- Environment-specific property files
- Profile-based configuration

## Configuration Metadata

IDE support is provided through `additional-spring-configuration-metadata.json` with:

- Property descriptions
- Default values
- Type information
- Validation hints

This enables autocomplete and validation in IDEs like IntelliJ IDEA and VS Code.