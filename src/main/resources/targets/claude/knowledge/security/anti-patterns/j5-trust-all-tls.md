---
name: kp-security-j5-trust-all-tls
description: "J5 — X509TrustManager Empty / Trust All (CWE-295). Vulnerable + fixed Java examples with rationale."
requires-capabilities: ["language.java.*"]
---

# J5: X509TrustManager Empty (Trust All)

**CWE:** CWE-295 — Improper Certificate Validation  
**Severity:** CRITICAL

## Vulnerable Code

```java
TrustManager[] trustAll = new TrustManager[] {
    new X509TrustManager() {
        public void checkClientTrusted(
                X509Certificate[] c, String a) {}
        public void checkServerTrusted(
                X509Certificate[] c, String a) {}
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
};
SSLContext ctx = SSLContext.getInstance("TLS");
ctx.init(null, trustAll, null);
```

## Fixed Code

```java
SSLContext ctx = SSLContext.getInstance("TLS");
TrustManagerFactory tmf =
        TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
tmf.init((KeyStore) null);
ctx.init(null, tmf.getTrustManagers(), null);
```

## Why it is dangerous

A trust-all manager disables TLS certificate validation, allowing man-in-the-middle attacks. An attacker on the network can intercept, read, and modify all HTTPS traffic without detection.
