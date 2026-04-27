/**
 * Driven Adapter — YAML stack profile configuration loading.
 *
 * <p>Implements the configuration repository output port. Reads and parses YAML-based stack profile
 * definitions using SnakeYAML, translating them into domain model objects.
 *
 * <p><strong>Dependency restrictions (RULE-002):</strong>
 *
 * <ul>
 *   <li>MUST implement a {@code domain.port.output} interface
 *   <li>MAY import SnakeYAML and Jackson classes
 *   <li>MAY depend on {@code domain.model} for configuration types
 *   <li>MUST NOT depend on other adapters or the CLI layer
 * </ul>
 *
 * @see dev.iadev.domain.port.output
 */
package dev.iadev.infrastructure.adapter.output.config;
