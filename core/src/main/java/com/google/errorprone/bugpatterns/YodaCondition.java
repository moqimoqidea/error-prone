/*
 * Copyright 2023 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.fixes.SuggestedFixes.qualifyType;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.staticEqualsInvocation;
import static com.google.errorprone.util.ASTHelpers.constValue;
import static com.google.errorprone.util.ASTHelpers.getNullnessValue;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.isStatic;
import static java.lang.String.format;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.BinaryTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.dataflow.nullnesspropagation.Nullness;
import com.google.errorprone.dataflow.nullnesspropagation.NullnessAnalysis;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.Objects;
import java.util.regex.Pattern;

/** See the summary. */
@BugPattern(
    summary =
        "The non-constant portion of an equals check generally comes first. Prefer"
            + " e.equals(CONSTANT) if e is non-null or Objects.equals(e, CONSTANT) if e may be",
    severity = WARNING)
public final class YodaCondition extends BugChecker
    implements BinaryTreeMatcher, MethodInvocationTreeMatcher {
  @Override
  public Description matchBinary(BinaryTree tree, VisitorState state) {
    switch (tree.getKind()) {
      case EQUAL_TO:
      case NOT_EQUAL_TO:
        return fix(
            tree,
            tree.getLeftOperand(),
            tree.getRightOperand(),
            /* provideNullSafeFix= */ false,
            state);
      default:
        return NO_MATCH;
    }
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (staticEqualsInvocation().matches(tree, state)) {
      return fix(
          tree,
          tree.getArguments().get(0),
          tree.getArguments().get(1),
          /* provideNullSafeFix= */ false,
          state);
    }
    if (instanceEqualsInvocation().matches(tree, state)) {
      ExpressionTree receiver = getReceiver(tree);
      if (receiver == null) {
        // call to equals implicitly qualified by `this`
        return NO_MATCH;
      }
      return fix(tree, receiver, tree.getArguments().get(0), /* provideNullSafeFix= */ true, state);
    }
    return NO_MATCH;
  }

  private Description fix(
      Tree tree,
      ExpressionTree lhs,
      ExpressionTree rhs,
      boolean provideNullSafeFix,
      VisitorState state) {
    if (yodaCondition(lhs, rhs)) {
      var description = buildDescription(lhs);
      if (provideNullSafeFix
          && !getNullnessValue(rhs, state, NullnessAnalysis.instance(state.context))
              .equals(Nullness.NONNULL)) {
        var fix = SuggestedFix.builder().setShortDescription("null-safe fix");
        description.addFix(
            fix.replace(
                    tree,
                    format(
                        "%s.equals(%s, %s)",
                        qualifyType(state, fix, Objects.class.getName()),
                        state.getSourceForNode(rhs),
                        state.getSourceForNode(lhs)))
                .build());
      }
      return description.addFix(SuggestedFix.swap(lhs, rhs)).build();
    }
    return NO_MATCH;
  }

  private static boolean yodaCondition(ExpressionTree lhs, ExpressionTree rhs) {
    ConstantKind l = seemsConstant(lhs);
    ConstantKind r = seemsConstant(rhs);
    return l.constness > r.constness;
  }

  enum ConstantKind {
    NULL(3),
    CONSTANT(2),
    CONSTANT_VARIABLE(1),
    NON_CONSTANT(0);

    final int constness;

    ConstantKind(int constness) {
      this.constness = constness;
    }
  }

  private static ConstantKind seemsConstant(Tree tree) {
    if (constValue(tree) != null) {
      return ConstantKind.CONSTANT;
    }
    if (tree.getKind().equals(Tree.Kind.NULL_LITERAL)) {
      return ConstantKind.NULL;
    }
    var symbol = getSymbol(tree);
    if (symbol instanceof VarSymbol
        && (symbol.isEnum()
            || (isStatic(symbol)
                && CONSTANT_CASE.matcher(symbol.getSimpleName().toString()).matches()))) {
      return ConstantKind.CONSTANT_VARIABLE;
    }
    return ConstantKind.NON_CONSTANT;
  }

  private static final Pattern CONSTANT_CASE = Pattern.compile("[A-Z0-9_]+");
}
