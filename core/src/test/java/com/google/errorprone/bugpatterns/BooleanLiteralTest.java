/*
 * Copyright 2025 The Error Prone Authors.
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

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BooleanLiteralTest {
  private final BugCheckerRefactoringTestHelper refactoringHelper =
      BugCheckerRefactoringTestHelper.newInstance(BooleanLiteral.class, getClass());

  @Test
  public void positive() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              void f() {
                System.err.println(Boolean.TRUE.toString());
                System.err.println(Boolean.FALSE.toString());
                System.err.println(Boolean.TRUE.booleanValue());
                System.err.println(Boolean.FALSE.booleanValue());
                System.err.println(Boolean.TRUE);
                System.err.println(Boolean.FALSE);
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              void f() {
                System.err.println("true");
                System.err.println("false");
                System.err.println(true);
                System.err.println(false);
                System.err.println(true);
                System.err.println(false);
              }
            }
            """)
        .doTest();
  }

  @Test
  public void methodRef() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            import java.util.function.Predicate;

            class Test {
              void f() {
                Predicate<Boolean> p = Boolean.TRUE::equals;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void moduleInfo() {
    refactoringHelper
        .addInputLines(
            "module-info.java",
            """
            module foo {
              requires java.base;
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void objectDoubleEqualsBooleanLiteral() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              boolean objectDoubleEqualsBooleanTrue(Object o) {
                return o == Boolean.TRUE;
              }

              boolean objectDoubleEqualsBooleanFalse(Object o) {
                return o == Boolean.FALSE;
              }

              boolean booleanTrueDoubleEqualsObject(Object o) {
                return Boolean.TRUE == o;
              }

              boolean booleanFalseDoubleEqualsObject(Object o) {
                return Boolean.FALSE == o;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void primitiveBooleanDoubleEqualsBooleanLiteral() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              boolean booleanDoubleEqualsBooleanTrue(boolean b) {
                return b == Boolean.TRUE;
              }

              boolean booleanDoubleEqualsBooleanFalse(boolean b) {
                return b == Boolean.FALSE;
              }

              boolean booleanTrueDoubleEqualsBoolean(boolean b) {
                return Boolean.TRUE == b;
              }

              boolean booleanFalseDoubleEqualsBoolean(boolean b) {
                return Boolean.FALSE == b;
              }
            }
            """)
        .addOutputLines(
            "Test.java",
            """
            class Test {
              boolean booleanDoubleEqualsBooleanTrue(boolean b) {
                return b == true;
              }

              boolean booleanDoubleEqualsBooleanFalse(boolean b) {
                return b == false;
              }

              boolean booleanTrueDoubleEqualsBoolean(boolean b) {
                return true == b;
              }

              boolean booleanFalseDoubleEqualsBoolean(boolean b) {
                return false == b;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void boxedBooleanDoubleEqualsBooleanLiteral() {
    refactoringHelper
        .addInputLines(
            "Test.java",
            """
            class Test {
              boolean booleanDoubleEqualsBooleanTrue(Boolean b) {
                return b == Boolean.TRUE;
              }

              boolean booleanDoubleEqualsBooleanFalse(Boolean b) {
                return b == Boolean.FALSE;
              }

              boolean booleanTrueDoubleEqualsBoolean(Boolean b) {
                return Boolean.TRUE == b;
              }

              boolean booleanFalseDoubleEqualsBoolean(Boolean b) {
                return Boolean.FALSE == b;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }

  @Test
  public void boxedResult() {
    refactoringHelper
        .allowBreakingChanges()
        .addInputLines(
            "Test.java",
            """
            class Test {
              Boolean f() {
                Boolean foo = null;
                Boolean b = true ? foo : Boolean.TRUE;
                return b;
              }
            }
            """)
        .expectUnchanged()
        .doTest();
  }
}
