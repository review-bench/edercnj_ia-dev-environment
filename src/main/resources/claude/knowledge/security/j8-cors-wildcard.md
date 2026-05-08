---
name: kp-security-j8-cors-wildcard
description: "J8 — CORS allowedOrigins(*) (CWE-942). Vulnerable + fixed Java examples with rationale."
requires-capabilities: ["language.java.*"]
---

# J8: CORS allowedOrigins("*")

**CWE:** CWE-942 — Permissive Cross-domain Policy with Untrusted Domains  
**Severity:** HIGH

## Vulnerable Code

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*")
                .allowCredentials(true);
    }
}
```

## Fixed Code

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://app.example.com")
                .allowedMethods("GET", "POST", "PUT")
                .allowCredentials(true);
    }
}
```

## Why it is dangerous

A wildcard CORS policy allows any website to make authenticated cross-origin requests to your API. An attacker can host a malicious page that reads sensitive data from your endpoints using the victim's cookies or tokens.
