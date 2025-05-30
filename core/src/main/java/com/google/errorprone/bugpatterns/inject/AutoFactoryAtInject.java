/*
 * Copyright 2016 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.InjectMatchers.IS_APPLICATION_OF_AT_INJECT;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.methodIsConstructor;
import static com.google.errorprone.util.ASTHelpers.findEnclosingNode;
import static com.google.errorprone.util.ASTHelpers.getConstructors;
import static com.sun.source.tree.Tree.Kind.CLASS;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.AnnotationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

/**
 * @author ronshapiro@google.com (Ron Shapiro)
 */
@BugPattern(
    summary = "@AutoFactory and @Inject should not be used in the same type.",
    severity = ERROR)
public class AutoFactoryAtInject extends BugChecker implements AnnotationTreeMatcher {

  private static final Matcher<Tree> HAS_AUTO_FACTORY_ANNOTATION =
      hasAnnotation("com.google.auto.factory.AutoFactory");

  @Override
  public final Description matchAnnotation(AnnotationTree annotationTree, VisitorState state) {
    if (!IS_APPLICATION_OF_AT_INJECT.matches(annotationTree, state)) {
      return Description.NO_MATCH;
    }

    Tree annotatedTree = getCurrentlyAnnotatedNode(state);
    if (!(annotatedTree instanceof MethodTree methodTree)
        || !methodIsConstructor().matches(methodTree, state)) {
      return Description.NO_MATCH;
    }

    ClassTree classTree = findEnclosingNode(state.getPath(), ClassTree.class);
    ImmutableList<Tree> potentiallyAnnotatedTrees =
        ImmutableList.<Tree>builder().add(classTree).addAll(getConstructors(classTree)).build();
    for (Tree potentiallyAnnotatedTree : potentiallyAnnotatedTrees) {
      if (HAS_AUTO_FACTORY_ANNOTATION.matches(potentiallyAnnotatedTree, state)
          && (potentiallyAnnotatedTree.getKind().equals(CLASS)
              || potentiallyAnnotatedTree.equals(annotatedTree))) {
        return describeMatch(annotationTree, SuggestedFix.delete(annotationTree));
      }
    }

    return Description.NO_MATCH;
  }

  // TODO(ronshapiro): consolidate uses
  private static Tree getCurrentlyAnnotatedNode(VisitorState state) {
    return state.getPath().getParentPath().getParentPath().getLeaf();
  }
}
