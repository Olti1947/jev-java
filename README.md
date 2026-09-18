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

[![Maven Central](https://img.shields.io/maven-central/v/io.github.olti1947/jev-java.svg)](https://central.sonatype.com/artifact/io.github.olti1947/jev-java)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://github.com/Olti1947/jev-java/actions/workflows/publish.yml/badge.svg)](https://github.com/Olti1947/jev-java/actions)