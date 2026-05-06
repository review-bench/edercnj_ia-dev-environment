# Rule 12 — Security Anti-Patterns (Java)

> Full vulnerable/fixed examples in KP: `Read src/main/resources/targets/claude/knowledge/security/anti-patterns/<jN-name>.md`

## Index (CWE + Severity)

| ID | Pattern | CWE | Severity |
| :--- | :--- | :--- | :--- |
| J1 | SQL Concatenation with String | CWE-89 | CRITICAL |
| J2 | Math.random() for Security | CWE-330 | HIGH |
| J3 | ObjectInputStream Without Whitelist | CWE-502 | CRITICAL |
| J4 | Password Hardcoded in String | CWE-798 | CRITICAL |
| J5 | X509TrustManager Empty (Trust All) | CWE-295 | CRITICAL |
| J6 | new File(userInput) Without Normalization | CWE-22 | HIGH |
| J7 | Exception Message in HTTP Response | CWE-209 | MEDIUM |
| J8 | CORS allowedOrigins("*") | CWE-942 | HIGH |

## Usage

When reviewing or implementing security-sensitive code, invoke:

```
Read src/main/resources/targets/claude/knowledge/security/anti-patterns/j{N}-{name}.md
```

for the relevant anti-pattern(s).
