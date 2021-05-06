/*
 * Copyright 2021 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.inlineme;

import com.google.common.base.CharMatcher;

/** Helper utility methods shared between multiple {@code @InlineMe} checkers. */
final class Helpers {

  private Helpers() {}

  private static final CharMatcher SEMI_COLON_MATCHER = CharMatcher.is(';');

  /** Normalizes a statement by stripping leading {@code "return "} and trailing semicolons. */
  static String normalize(String statement) {
    // strip trailing semicolons
    statement = SEMI_COLON_MATCHER.trimTrailingFrom(statement);
    // TODO(kak): avoid the need to strip the `return` by passing returnTree.getExpression() instead
    // strip the leading `return` keyword (if present)
    return statement.replaceFirst("^return\\s+", "");
  }
}