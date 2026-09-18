---
name: Bug report
about: Create a report to help us improve jev-java
title: 'bug: [Short description of the issue]'
labels: 'bug'
assignees: ''
---

## Description
A clear and concise description of what the bug is.

## Expected Behavior
A clear description of what you expected to happen.

## Current Behavior
What actually happened. Include error messages, stack traces, or unexpected outputs here.

## Steps to Reproduce
Steps to reproduce the behavior:
1. Initialize `JevClient` with ...
2. Call method `client.evaluate(...)` with input payload ...
3. See error ...

## Code Snippet / Minimal Reproducible Example
```java
// Paste a minimal Java code snippet demonstrating the bug
JevClient client = JevClient.builder()
        .apiKey("test")
        .build();

// ...
```

## Additional Context
Add here any other information you may find necessary.