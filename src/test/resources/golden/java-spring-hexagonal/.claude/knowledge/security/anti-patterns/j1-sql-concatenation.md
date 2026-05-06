---
name: kp-security-j1-sql-concatenation
description: "J1 — SQL Concatenation (CWE-89 SQL Injection). Vulnerable + fixed Java examples with rationale."
requires-capabilities: ["language.java.*"]
---

# J1: SQL Concatenation with String

**CWE:** CWE-89 — SQL Injection  
**Severity:** CRITICAL

## Vulnerable Code

```java
public List<User> findByName(String name) {
    String sql = "SELECT * FROM users WHERE name = '"
            + name + "'";
    return jdbcTemplate.query(sql, userMapper);
}
```

## Fixed Code

```java
public List<User> findByName(String name) {
    String sql = "SELECT * FROM users WHERE name = ?";
    return jdbcTemplate.query(sql, userMapper, name);
}
```

## Why it is dangerous

An attacker can inject arbitrary SQL through the `name` parameter (e.g., `' OR 1=1 --`), bypassing authentication, exfiltrating data, or dropping tables. Parameterized queries ensure user input is always treated as data, never as executable SQL.
