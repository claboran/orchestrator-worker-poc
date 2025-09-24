# Testcontainers Setup Guide

This document explains the streamlined testcontainer setup for the orchestrator-worker-poc project.

## Overview

The project now provides two streamlined base test classes that use proper testcontainer annotations:

1. **`BaseIntegrationTest`** - Full integration testing with PostgreSQL + ElasticMQ
2. **`BaseDatabaseTest`** - Database-only testing with PostgreSQL

## Base Test Classes

### BaseIntegrationTest

Use this for tests that need both database and SQS functionality:

```kotlin
@SpringBootTest
class MyIntegrationTest : BaseIntegrationTest() {
    @Test
    fun myTest() {
        // Test has access to both PostgreSQL and ElasticMQ
    }
}
```

Features:
- PostgreSQL container (postgres:16)
- ElasticMQ container (softwaremill/elasticmq:1.6.0)
- Automatic Spring property configuration
- SQS queue creation (job-control-queue, job-worker-queue)

### BaseDatabaseTest

Use this for tests that only need database functionality:

```kotlin
@SpringBootTest
class MyDatabaseTest : BaseDatabaseTest() {
    @Test
    fun myDatabaseTest() {
        // Test has access to PostgreSQL only
    }
}
```

Features:
- PostgreSQL container (postgres:16)
- Automatic Spring datasource configuration

## Benefits of the New Setup

1. **Annotation-based**: Uses `@Testcontainers` and `@Container` annotations properly
2. **Consistent**: Unified approach across all tests
3. **Efficient**: Containers are shared across test methods in the same class
4. **Flexible**: Choose the right base class for your needs
5. **Clean**: No manual container lifecycle management

## Migration Guide

### For New Tests

Simply extend the appropriate base class:
- Need both database and SQS? → Extend `BaseIntegrationTest`
- Need only database? → Extend `BaseDatabaseTest`

### For Existing Tests

The existing approaches still work and are compatible:

1. **Tests extending `AbstractElasticMqTest`**: Continue to work as before
2. **Tests using direct `@Testcontainers`**: Continue to work as before
3. **Main application test**: Now uses `BaseIntegrationTest` and works properly

## Example Usage

### Full Integration Test
```kotlin
@SpringBootTest
@ActiveProfiles("test", "orchestrator")
class MyServiceIntegrationTest : BaseIntegrationTest() {
    
    @Autowired
    private lateinit var myService: MyService
    
    @Test
    fun `should process messages correctly`() {
        // Test implementation with access to both DB and SQS
    }
}
```

### Database-Only Test
```kotlin
@SpringBootTest
class MyRepositoryTest : BaseDatabaseTest() {
    
    @Autowired
    private lateinit var myRepository: MyRepository
    
    @Test
    fun `should save and retrieve entities`() {
        // Test implementation with access to PostgreSQL only
    }
}
```

## Container Configuration

### PostgreSQL
- Image: `postgres:16`
- Database: `testdb`
- Username: `test`
- Password: `test`

### ElasticMQ
- Image: `softwaremill/elasticmq:1.6.0`
- Port: 9324
- Queues: `job-control-queue`, `job-worker-queue`

All properties are automatically configured via `@DynamicPropertySource`.

## Troubleshooting

1. **Docker not running**: Ensure Docker is running before running tests
2. **Port conflicts**: Testcontainers automatically assigns random ports
3. **Slow startup**: First run downloads containers; subsequent runs are faster
4. **Memory issues**: Consider increasing Docker memory allocation

## Legacy Support

The existing `AbstractElasticMqTest` and `ElasticMqTestContainer` utilities remain available for backward compatibility. However, new tests should use the streamlined base classes for consistency.