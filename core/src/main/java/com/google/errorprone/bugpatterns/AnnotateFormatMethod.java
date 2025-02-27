/*
 * Copyright 2018 The Error Prone Authors.
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

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.MoreCollectors.toOptional;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.BugPattern.StandardTags.FRAGILE_CODE;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.AnnotationNames.FORMAT_METHOD_ANNOTATION;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Detects occurrences of pairs of parameters being passed straight through to {@link String#format}
 * from a method not annotated with {@link com.google.errorprone.annotations.FormatMethod}.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    summary =
        "This method uses a pair of parameters as a format string and its arguments, but the"
            + " enclosing method wasn't annotated @FormatMethod. Doing so gives compile-time rather"
            + " than run-time protection against malformed format strings.",
    tags = FRAGILE_CODE,
    severity = WARNING)
public final class AnnotateFormatMethod extends BugChecker implements MethodInvocationTreeMatcher {

  private static final String REORDER =
      " (The parameters of this method would need to be reordered to make the format string and "
          + "arguments the final parameters before the @FormatMethod annotation can be used.)";

  private static final Matcher<ExpressionTree> STRING_FORMAT =
      staticMethod().onClass("java.lang.String").named("format");
  private static final Matcher<ExpressionTree> FORMATTED =
      instanceMethod().onExactClass("java.lang.String").named("formatted");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    VarSymbol formatString;
    VarSymbol formatArgs;
    if (STRING_FORMAT.matches(tree, state)) {
      if (tree.getArguments().size() != 2) {
        return Description.NO_MATCH;
      }
      formatString = asSymbol(tree.getArguments().get(0));
      formatArgs = asSymbol(tree.getArguments().get(1));
    } else if (FORMATTED.matches(tree, state)) {
      if (tree.getArguments().size() != 1) {
        return Description.NO_MATCH;
      }
      formatString = asSymbol(getReceiver(tree));
      formatArgs = asSymbol(tree.getArguments().get(0));
    } else {
      return Description.NO_MATCH;
    }
    if (formatString == null || formatArgs == null) {
      return Description.NO_MATCH;
    }
    MethodTree enclosingMethod = ASTHelpers.findEnclosingNode(state.getPath(), MethodTree.class);
    if (enclosingMethod == null
        || !ASTHelpers.getSymbol(enclosingMethod).isVarArgs()
        || hasAnnotation(enclosingMethod, FORMAT_METHOD_ANNOTATION, state)) {
      return Description.NO_MATCH;
    }
    List<? extends VariableTree> enclosingParameters = enclosingMethod.getParameters();
    Optional<? extends VariableTree> formatParameter =
        findParameterWithSymbol(enclosingParameters, formatString);
    Optional<? extends VariableTree> argumentsParameter =
        findParameterWithSymbol(enclosingParameters, formatArgs);
    if (!formatParameter.isPresent() || !argumentsParameter.isPresent()) {
      return Description.NO_MATCH;
    }
    if (!argumentsParameter.get().equals(getLast(enclosingParameters))) {
      return Description.NO_MATCH;
    }
    // We can only generate a fix if the format string is the penultimate parameter.
    boolean fixable =
        formatParameter.get().equals(enclosingParameters.get(enclosingParameters.size() - 2));
    return buildDescription(enclosingMethod)
        .setMessage(fixable ? message() : (message() + REORDER))
        .build();
  }

  private static Optional<? extends VariableTree> findParameterWithSymbol(
      List<? extends VariableTree> parameters, Symbol symbol) {
    return parameters.stream()
        .filter(parameter -> symbol.equals(ASTHelpers.getSymbol(parameter)))
        .collect(toOptional());
  }

  private static @Nullable VarSymbol asSymbol(ExpressionTree tree) {
    Symbol symbol = ASTHelpers.getSymbol(tree);
    return symbol instanceof VarSymbol varSymbol ? varSymbol : null;
  }
}
