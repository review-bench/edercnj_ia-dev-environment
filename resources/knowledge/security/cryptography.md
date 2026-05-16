# Cryptography

Algorithm choices, key management, and TLS configuration. Avoid reinventing crypto — use vetted libraries and the algorithms below.

## TLS / Transport

| Setting | Value |
|---------|-------|
| Minimum protocol | TLS 1.2 |
| Preferred protocol | TLS 1.3 |
| Forbidden protocols | SSL 2.0/3.0, TLS 1.0, TLS 1.1 |
| Cipher suites (TLS 1.2) | ECDHE-ECDSA/RSA with AES-GCM or ChaCha20-Poly1305 |
| Forbidden ciphers | NULL, EXPORT, anonymous, RC4, DES, 3DES, MD5-based MACs |
| Certificate validation | Always on; never disable trust manager |
| OCSP stapling | Enabled |
| HSTS | `max-age=63072000; includeSubDomains; preload` |

mTLS is required for service-to-service in regulated environments (PCI CDE, HIPAA). Client certs rotate with short lifetimes (≤90 days).

## Symmetric Encryption

| Use case | Algorithm | Key size | Notes |
|----------|-----------|----------|-------|
| Confidential data at rest | AES-256-GCM | 256 bit | Authenticated encryption — preferred |
| Confidential data at rest (FIPS-only) | AES-256-CBC + HMAC-SHA-256 | 256 bit | Encrypt-then-MAC |
| Restricted data at rest | AES-256-GCM with per-record IV | 256 bit | IV must be unique per encryption with same key |
| Field-level encryption (PCI) | AES-256-GCM via envelope encryption with KMS | 256 bit | DEK encrypted with KMS-held KEK |

IVs / nonces: 12 bytes for GCM, never reused with the same key. Use `SecureRandom` or a counter scheme audited for collision resistance.

## Asymmetric Encryption and Signing

| Use case | Algorithm | Key size |
|----------|-----------|----------|
| Signing tokens (JWT) | EdDSA (Ed25519) | 256 bit |
| Signing tokens (legacy) | RSA-PSS or RSA-SHA256 | 3072 bit minimum |
| Encrypting symmetric keys (envelope) | RSA-OAEP-SHA256 | 3072 bit minimum |
| Modern encryption | X25519 + AES-GCM (NaCl/libsodium) | 256 bit |
| Forbidden | RSA-PKCS1-v1.5 (padding oracle), DSA, ECDSA with secp192r1 | — |

## Hashing

| Use case | Algorithm | Notes |
|----------|-----------|-------|
| Password storage | Argon2id (preferred), scrypt, or bcrypt (cost ≥ 12) | Per-user salt, no global pepper required if KMS-stored |
| HMAC | HMAC-SHA-256 | For message integrity with shared key |
| File integrity | SHA-256 or SHA-3-256 | For checksums, not passwords |
| Content addressing | SHA-256 | For deduplication, cache keys |
| Forbidden for any new use | MD5, SHA-1 | Even for non-security uses, prefer SHA-256 to avoid drift |

Constant-time comparison required when comparing hashes, tokens, or MACs (`MessageDigest.isEqual`, not `String.equals`).

## Random Number Generation

| Use case | API |
|----------|-----|
| Tokens, session IDs, nonces, IVs | `SecureRandom.getInstanceStrong()` |
| UUIDs (non-security) | `UUID.randomUUID()` (uses SecureRandom internally) |
| Forbidden | `java.util.Random`, `Math.random()`, `ThreadLocalRandom` for any security purpose |

See Rule 11 PRH-04 and Rule 12 J2 for project-specific prohibitions.

## Key Management

### Storage

| Class | Storage |
|-------|---------|
| TLS server certs | Filesystem with restricted perms, loaded at startup |
| Application secrets (DB pwd, API keys) | Vault / AWS Secrets Manager / GCP Secret Manager |
| Encryption KEKs (key-encryption keys) | KMS / HSM — never extractable |
| DEKs (data-encryption keys) | Encrypted with KEK and stored alongside ciphertext |
| Private signing keys | KMS with `Sign` permission; key material never leaves KMS |

### Rotation

| Key type | Rotation cadence | Mechanism |
|----------|------------------|-----------|
| TLS server cert | 90 days (Let's Encrypt) or 1 year (commercial) | Automated reload |
| JWT signing key | 30 days | Multi-key support: sign with current, verify with current + previous N |
| Application secrets | 90 days minimum, on suspicion immediately | Rolling restart |
| KEK | Annual or per compliance requirement | Re-encrypt DEKs; ciphertext stays put |
| DEK | Per record / per envelope; effectively never rotated as a "key" | Re-encrypt-on-update if requirements change |

### Destruction

- Old keys retained only for ciphertext-decrypt windows; destroyed when no live ciphertext depends on them.
- HSM/KMS deletion is final and audited.
- Filesystem keys overwritten with `shred` or equivalent before disposal of media.

## Envelope Encryption Pattern

For Restricted data at rest:

1. Generate a fresh DEK (32 bytes from `SecureRandom`) per record.
2. Encrypt the plaintext with DEK using AES-256-GCM; store ciphertext + IV.
3. Encrypt the DEK with the KEK via KMS `Encrypt` API; store wrapped DEK alongside ciphertext.
4. On read: KMS `Decrypt` unwraps the DEK; decrypt ciphertext.

Benefits: KEK rotation re-wraps DEKs only (cheap); KEK never leaves KMS; audit trail per record decryption.

## What NOT to Do

- Roll your own crypto. Use JCA/JCE, libsodium, BouncyCastle FIPS edition, or KMS APIs.
- Hardcode keys, IVs, salts, or seeds (Rule 11 PRH-07, Rule 12 J4).
- Use ECB mode for anything. AES-ECB leaks plaintext patterns; never use it.
- Reuse IV / nonce with the same key in GCM. One reuse can recover the plaintext XOR.
- Trust client-side encryption alone. Encrypt-on-server is the contract; client encryption is extra defense, not a replacement.

## Cross-References

- `security-principles.md` (defense in depth, classification)
- `application-security.md` (TLS configuration in context)
- Rule 11 — PCI-DSS prohibitions (PAN encryption, no Math.random)
- Rule 12 — J2 (Math.random), J5 (TLS trust-all)
- NIST SP 800-131A (transitioning algorithms)
- NIST SP 800-63B (password storage)
