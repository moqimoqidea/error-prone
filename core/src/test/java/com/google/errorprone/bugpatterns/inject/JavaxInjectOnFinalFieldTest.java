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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author sgoldfeder@google.com (Steven Goldfeder)
 */
@RunWith(JUnit4.class)
public class JavaxInjectOnFinalFieldTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(JavaxInjectOnFinalField.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "JavaxInjectOnFinalFieldPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.inject.testdata;

            import javax.inject.Inject;

            /**
             * @author sgoldfeder@google.com (Steven Goldfeder)
             */
            public class JavaxInjectOnFinalFieldPositiveCases {

              /** Class has a final injectable(javax.inject.Inject) field. */
              public class TestClass1 {
                // BUG: Diagnostic contains:
                @Inject public final int n = 0;
              }
            }
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "JavaxInjectOnFinalFieldNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.inject.testdata;

            import javax.inject.Inject;

            /**
             * @author sgoldfeder@google.com (Steven Goldfeder)
             */
            public class JavaxInjectOnFinalFieldNegativeCases {

              /** Class has no final fields or @Inject annotations. */
              public class TestClass1 {}

              /** Class has a final field that is not injectable. */
              public class TestClass2 {
                public final int n = 0;
              }

              /** Class has an injectable(javax.inject.Inject) field that is not final. */
              public class TestClass3 {
                @Inject public int n;
              }

              /** Class has an injectable(javax.inject.Inject), final method. */
              public class TestClass4 {
                @Inject
                final void method() {}
              }
            }\
            """)
        .doTest();
  }
}
