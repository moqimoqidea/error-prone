/*
 * Copyright 2013 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.inject;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.InjectMatchers.GUICE_SCOPE_ANNOTATION;
import static com.google.errorprone.matchers.InjectMatchers.IS_DAGGER_COMPONENT;
import static com.google.errorprone.matchers.InjectMatchers.JAVAX_SCOPE_ANNOTATION;
import static com.google.errorprone.matchers.Matchers.symbolHasAnnotation;
import static javax.lang.model.element.Modifier.ABSTRACT;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@BugPattern(
    name = "InjectScopeAnnotationOnInterfaceOrAbstractClass",
    summary = "Scope annotation on an interface or abstract class is not allowed",
    severity = WARNING)
public class ScopeAnnotationOnInterfaceOrAbstractClass extends BugChecker
    implements AnnotationTreeMatcher {

  /**
   * Matches annotations that are themselves annotated with {@code @ScopeAnnotation(Guice)} or
   * {@code @Scope(Javax)}.
   */
  private static final Matcher<AnnotationTree> SCOPE_ANNOTATION_MATCHER =
      Matchers.<AnnotationTree>anyOf(
          symbolHasAnnotation(GUICE_SCOPE_ANNOTATION), symbolHasAnnotation(JAVAX_SCOPE_ANNOTATION));

  private static final Matcher<ClassTree> INTERFACE_AND_ABSTRACT_TYPE_MATCHER =
      new Matcher<ClassTree>() {
        @Override
        public boolean matches(ClassTree classTree, VisitorState state) {
          return classTree.getModifiers().getFlags().contains(ABSTRACT)
              || (ASTHelpers.getSymbol(classTree).flags() & Flags.INTERFACE) != 0;
        }
      };

  @Override
  public final Description matchAnnotation(AnnotationTree annotationTree, VisitorState state) {
    Tree modified = getCurrentlyAnnotatedNode(state);
    if (SCOPE_ANNOTATION_MATCHER.matches(annotationTree, state)
        && modified instanceof ClassTree classTree
        && !IS_DAGGER_COMPONENT.matches(classTree, state)
        && INTERFACE_AND_ABSTRACT_TYPE_MATCHER.matches(classTree, state)) {
      return describeMatch(annotationTree, SuggestedFix.delete(annotationTree));
    }
    return Description.NO_MATCH;
  }

  private static Tree getCurrentlyAnnotatedNode(VisitorState state) {
    return state.getPath().getParentPath().getParentPath().getLeaf();
  }
}
