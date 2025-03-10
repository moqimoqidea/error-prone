/*
 * Copyright 2014 The Error Prone Authors.
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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author lowasser@google.com (Louis Wasserman)
 */
@RunWith(JUnit4.class)
public class StringBuilderInitWithCharTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(StringBuilderInitWithChar.class, getClass());

  @Test
  public void positiveCases() {
    compilationHelper
        .addSourceLines(
            "StringBuilderInitWithCharPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author lowasser@google.com (Louis Wasserman)
             */
            public class StringBuilderInitWithCharPositiveCases {
              {
                // BUG: Diagnostic contains: new StringBuilder("a")
                new StringBuilder('a');
                // BUG: Diagnostic contains: new StringBuilder("\\"")
                new StringBuilder('"');
                char c = 'c';
                // BUG: Diagnostic contains: new StringBuilder().append(c)
                new StringBuilder(c);
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCases() {
    compilationHelper
        .addSourceLines(
            "StringBuilderInitWithCharNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author lowasser@google.com (Louis Wasserman)
             */
            public class StringBuilderInitWithCharNegativeCases {
              {
                new StringBuilder("a");
                new StringBuilder(5);
                new StringBuilder();
              }
            }\
            """)
        .doTest();
  }
}
