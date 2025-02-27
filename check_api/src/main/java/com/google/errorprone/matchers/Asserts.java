/*
 * Copyright 2015 The Error Prone Authors.
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

package com.google.errorprone.matchers;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.StatementTree;

/** Matches assert statements which have a condition expression matched by the given matcher. */
public class Asserts implements Matcher<StatementTree> {

  private final Matcher<ExpressionTree> expressionMatcher;

  public Asserts(Matcher<ExpressionTree> expressionMatcher) {
    this.expressionMatcher = expressionMatcher;
  }

  @Override
  public boolean matches(StatementTree statementTree, VisitorState state) {
    return statementTree instanceof AssertTree assertTree
        && expressionMatcher.matches(assertTree.getCondition(), state);
  }
}
