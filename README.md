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
        new Noul("is_angry", "Is the customer expressing anger?")
    )
);

response.choice("intent").choice();        // "cancel"
response.choice("intent").probabilities(); // {billing=0.01, support=0.0, cancel=0.99}
response.noul("is_angry").noul();          // 0.79
response.noul("is_angry").isTrue(0.7);     // true
```

## Vercel AI Gateway

Jev is also available through the [Vercel AI Gateway](https://vercel.com/docs/ai-gateway/modalities/evaluation),
which is handy while direct TypeSafe API access is waitlisted. Use an AI Gateway
API key (`vck_...`) and enable gateway mode on the builder — the rest of the API
is unchanged:

```java
JevClient jev = JevClient.builder()
    .apiKey(System.getenv("AI_GATEWAY_API_KEY"))
    .vercelGateway()
    .build();
```

Notes:
- The gateway model id defaults to `typesafe-ai/jev` and can be overridden with `.gatewayModel(...)`.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.olti1947/jev-java.svg)](https://central.sonatype.com/artifact/io.github.olti1947/jev-java)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://github.com/Olti1947/jev-java/actions/workflows/publish.yml/badge.svg)](https://github.com/Olti1947/jev-java/actions)