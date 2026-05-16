---
name: kp-security-j7-exception-leakage
description: "J7 — Exception Message in HTTP Response (CWE-209). Vulnerable + fixed Java examples with rationale."
requires-capabilities: ["language.java.*"]
---

# J7: Exception Message in HTTP Response

**CWE:** CWE-209 — Generation of Error Message Containing Sensitive Information  
**Severity:** MEDIUM

## Vulnerable Code

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<String> handleError(Exception e) {
    return ResponseEntity.status(500)
            .body(e.getMessage());
}
```

## Fixed Code

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleError(
        Exception e) {
    log.error("Unhandled exception", e);
    return ResponseEntity.status(500)
            .body(new ErrorResponse(
                    "Internal server error", "ERR-500"));
}
```

## Why it is dangerous

Exception messages may contain internal class names, SQL queries, file paths, or stack traces that reveal the application's technology stack and internal structure. Attackers use this information to craft targeted exploits.
