/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.constructor;
import static com.google.errorprone.predicates.TypePredicates.isDescendantOf;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasDirectAnnotationWithSimpleName;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.suppliers.Supplier;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(summary = "ThreadLocals should be stored in static fields", severity = WARNING)
public class ThreadLocalUsage extends BugChecker implements NewClassTreeMatcher {

  private static final Matcher<ExpressionTree> NEW_THREAD_LOCAL =
      constructor().forClass(isDescendantOf("java.lang.ThreadLocal"));

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    if (!NEW_THREAD_LOCAL.matches(tree, state)) {
      return NO_MATCH;
    }
    if (wellKnownTypeArgument(tree, state)) {
      return NO_MATCH;
    }
    Tree parent = state.getPath().getParentPath().getLeaf();
    if (!(parent instanceof VariableTree variableTree)) {
      // If the ThreadLocal is created outside of a field we can't easily make assumptions about its
      // scope.
      return NO_MATCH;
    }
    VarSymbol sym = getSymbol(variableTree);
    if (sym.isStatic()) {
      return NO_MATCH;
    }
    if (Streams.stream(state.getPath())
        .filter(ClassTree.class::isInstance)
        .map(ClassTree.class::cast)
        .anyMatch(
            c -> {
              if (hasDirectAnnotationWithSimpleName(getSymbol(c), "Singleton")) {
                return true;
              }
              Type scopeType = COM_GOOGLE_INJECT_SCOPE.get(state);
              if (isSubtype(getType(c), scopeType, state)) {
                return true;
              }
              return false;
            })) {
      // The instance X thread issue doesn't apply if there's only one instance.
      return NO_MATCH;
    }
    return describeMatch(tree);
  }

  private static final ImmutableSet<String> WELL_KNOWN_TYPES =
      ImmutableSet.of(
          "java.lang.String",
          "java.lang.Boolean",
          "java.lang.Long",
          "java.lang.Integer",
          "java.lang.Short",
          "java.lang.Character",
          "java.lang.Float",
          "java.lang.Double");

  /** Ignore some common ThreadLocal type arguments that are fine to have per-instance copies of. */
  private static boolean wellKnownTypeArgument(NewClassTree tree, VisitorState state) {
    Type type = getType(tree);
    if (type == null) {
      return false;
    }
    type = state.getTypes().asSuper(type, JAVA_LANG_THREADLOCAL.get(state));
    if (type == null) {
      return false;
    }
    if (type.getTypeArguments().isEmpty()) {
      return false;
    }
    Type argType = getOnlyElement(type.getTypeArguments());
    if (WELL_KNOWN_TYPES.contains(argType.asElement().getQualifiedName().toString())) {
      return true;
    }
    if (isSubtype(argType, JAVA_TEXT_DATEFORMAT.get(state), state)) {
      return true;
    }
    return false;
  }

  private static final Supplier<Symbol> JAVA_LANG_THREADLOCAL =
      VisitorState.memoize(state -> state.getSymbolFromString("java.lang.ThreadLocal"));

  private static final Supplier<Type> COM_GOOGLE_INJECT_SCOPE =
      VisitorState.memoize(state -> state.getTypeFromString("com.google.inject.Scope"));

  private static final Supplier<Type> JAVA_TEXT_DATEFORMAT =
      VisitorState.memoize(state -> state.getTypeFromString("java.text.DateFormat"));
}
