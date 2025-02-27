/*
 * Copyright 2019 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.streamSuperMethods;
import static com.google.errorprone.util.AnnotationNames.CAN_IGNORE_RETURN_VALUE_ANNOTATION;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.threadsafety.ConstantExpressions;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.suppliers.Supplier;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import javax.inject.Inject;

/** A {@link BugChecker}; see the associated {@link BugPattern} for details. */
@BugPattern(
    summary =
        "Returned Rx objects must be checked. Ignoring a returned Rx value means it is never "
            + "scheduled for execution",
    explanation =
        "Methods that return an ignored [Observable | Single | Flowable | Maybe ] generally "
            + "indicate errors.\n\nIf you don’t check the return value of these methods, the "
            + "observables may never execute. It also means the error case is not being handled",
    severity = WARNING)
public final class RxReturnValueIgnored extends AbstractReturnValueIgnored {

  private static boolean hasCirvAnnotation(ExpressionTree tree, VisitorState state) {
    Symbol untypedSymbol = getSymbol(tree);
    if (!(untypedSymbol instanceof MethodSymbol sym)) {
      return false;
    }

    // Directly has @CanIgnoreReturnValue
    if (hasAnnotation(sym, CAN_IGNORE_RETURN_VALUE_ANNOTATION, state)) {
      return true;
    }

    // If a super-class's method is annotated with @CanIgnoreReturnValue, we only honor that
    // if the super-type returned the exact same type. This lets us catch issues where a
    // superclass was annotated with @CanIgnoreReturnValue but the parent did not intend to
    // return an Rx type
    return streamSuperMethods(sym, state.getTypes())
        .anyMatch(
            superSym ->
                hasAnnotation(superSym, CAN_IGNORE_RETURN_VALUE_ANNOTATION, state)
                    && superSym.getReturnType().tsym.equals(sym.getReturnType().tsym));
  }

  private static boolean isExemptedMethod(ExpressionTree tree, VisitorState state) {
    Symbol sym = getSymbol(tree);
    if (!(sym instanceof MethodSymbol)) {
      return false;
    }

    // Currently the only exempted method is Map.put().
    return ASTHelpers.isSubtype(sym.owner.type, JAVA_UTIL_MAP.get(state), state)
        && sym.name.contentEquals("put");
  }

  private static final Matcher<ExpressionTree> MATCHER =
      allOf(
          Matchers.kindIs(Kind.METHOD_INVOCATION),
          anyOf(
              // RxJava2
              isSubtypeOf("io.reactivex.Observable"),
              isSubtypeOf("io.reactivex.Flowable"),
              isSubtypeOf("io.reactivex.Single"),
              isSubtypeOf("io.reactivex.Maybe"),
              isSubtypeOf("io.reactivex.Completable"),
              // RxJava1
              isSubtypeOf("rx.Observable"),
              isSubtypeOf("rx.Single"),
              isSubtypeOf("rx.Completable")),
          not(
              anyOf(
                  RxReturnValueIgnored::hasCirvAnnotation,
                  RxReturnValueIgnored::isExemptedMethod)));

  @Inject
  RxReturnValueIgnored(ConstantExpressions constantExpressions) {
    super(constantExpressions);
  }

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    Description description = super.matchMethodInvocation(tree, state);
    return description.equals(Description.NO_MATCH) ? Description.NO_MATCH : describeMatch(tree);
  }

  @Override
  public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
    Description description = super.matchMemberReference(tree, state);
    return description.equals(Description.NO_MATCH) ? Description.NO_MATCH : describeMatch(tree);
  }

  @Override
  public Matcher<? super ExpressionTree> specializedMatcher() {
    return MATCHER;
  }

  private static final Supplier<Type> JAVA_UTIL_MAP =
      VisitorState.memoize(state -> state.getTypeFromString("java.util.Map"));
}
