/**
 * 애플리케이션 계층.
 * <ul>
 *   <li>provided/: 외부에 제공할 Use Case 인터페이스</li>
 *   <li>required/: 외부에 요구할 포트 인터페이스 (Repository, JwtIssuer 등)</li>
 * </ul>
 * <p>
 * 의존 가능: domain. 의존 불가: adapter.
 */
@org.jspecify.annotations.NullMarked
package com.kim.starter.application;
