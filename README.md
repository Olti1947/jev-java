# Jev Java SDK

Idiomatic Java 17+ client library for TypeSafe AI's **Jev** System One decision engine.

## Installation

### Maven
```xml
<dependency>
    <groupId>io.github.olti1947</groupId>
    <artifactId>jev-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle
```groovy
implementation 'io.github.olti1947:jev-java:1.0.0'
```

## Quick Start

```java
JevClient jev = JevClient.create(System.getenv("TYPESAFE_API_KEY"));

JevResponse response = jev.evaluate(
    "Cancel my subscription immediately!",
    List.of(
        new Choice("intent", "Select classification", List.of("billing", "support", "cancel")),
        new Noul("is_angry", "Is the customer express anger?")
    )
);
```