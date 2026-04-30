/**
 * Driven Adapter — Pebble template rendering.
 *
 * <p>Implements the template rendering output port using the Pebble template engine. Translates
 * domain model objects into rendered file content via template evaluation.
 *
 * <p><strong>Dependency restrictions (RULE-002):</strong>
 *
 * <ul>
 *   <li>MUST implement a {@code domain.port.output} interface
 *   <li>MAY import Pebble framework classes
 *   <li>MAY depend on {@code domain.model} for template data
 *   <li>MUST NOT depend on other adapters or the CLI layer
 * </ul>
 *
 * @see dev.iadev.domain.port.output
 */
package dev.iadev.infrastructure.adapter.output.template;
