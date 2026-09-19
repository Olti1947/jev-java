# Jev Java SDK

Idiomatic Java 17+ client library for TypeSafe AI's **Jev** System One decision engine.

## Installation

### Maven
```xml
<dependency>
    <groupId>io.github.olti1947</groupId>
    <artifactId>jev-java</artifactId>
    <version>2.0.0</version>
</dependency>
```

### Gradle
```groovy
implementation 'io.github.olti1947:jev-java:2.0.0'
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

## Structured instructions and criteria

Everywhere a primitive takes a string, it also accepts a structured value. Plain
strings keep working exactly as before; use structured values when you need to
give Jev richer context. See the
[advanced primitives docs](https://docs.typesafe.ai/primitives/advanced).

```java
// Structured instructions: any map (question, focus, inspect, field, ...)
Noul invoiceCheck = Noul.builder("invoice_number_is_correct")
    .instructions(Map.of(
        "field", Map.of("name", "invoice_number", "type", "string"),
        "extracted_value", "4471",
        "question", "Does `extracted_value` match the `field` as it appears in `source_text`?"))
    .build();

// Choice options with what / not_for / examples boundaries
Choice department = Choice.builder("department")
    .instructions(Map.of("question", "Which team should handle this message?"))
    .candidate("billing", Rubric.of("Charges, invoices, refunds", "I was charged twice")
        .notFor("Order tracking or account access"))
    .candidate("orders", "Order status, delivery, or returns")   // strings still work
    .candidate("other")                                          // no description
    .build();

// Score levels with a summary and signals (levels run low -> high)
Score prScope = Score.builder("pr_scope")
    .instructions("How focused is this pull request on a single change?")
    .addCriteriaLevel(ScoreLevel.of("One change, clearly stated", "A single fix or feature"))
    .addCriteriaLevel(ScoreLevel.of("Several changes bundled together", "Two or more unrelated fixes"))
    .build();

// Noul true / false boundaries
Noul credentials = Noul.builder("requests_credentials")
    .instructions("Does the message ask for a sensitive credential?")
    .criteria(
        Rubric.of("Asks the recipient to send a password or one-time code", "Send us the 6-digit code"),
        Rubric.of("No sensitive credential is requested", "Reset your password from settings"))
    .build();
```

`Rubric` and `ScoreLevel` are typed helpers, but any `Map`/`List` works too, so
nested category maps and custom keys are supported. Constructors taking plain
strings (`new Noul(name, "question")`, `new Score(name, "q", "low", "high")`) are
unchanged.

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