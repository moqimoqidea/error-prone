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
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@RunWith(JUnit4.class)
public class PrimitiveArrayPassedToVarargsMethodTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(PrimitiveArrayPassedToVarargsMethod.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "PrimitiveArrayPassedToVarargsMethodPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            import java.util.Arrays;

            /**
             * @author eaftan@google.com (Eddie Aftandilian)
             */
            public class PrimitiveArrayPassedToVarargsMethodPositiveCases {

              public void objectVarargsMethod(Object... objs) {}

              public <T> void genericVarargsMethod(T... genericArrays) {}

              public void objectVarargsMethodWithMultipleParams(Object obj1, Object... objs) {}

              public void doIt() {
                int[] intArray = {1, 2, 3};

                // BUG: Diagnostic contains:
                objectVarargsMethod(intArray);

                // BUG: Diagnostic contains:
                genericVarargsMethod(intArray);

                // BUG: Diagnostic contains:
                objectVarargsMethodWithMultipleParams(new Object(), intArray);

                // BUG: Diagnostic contains:
                Arrays.asList(intArray);
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "PrimitiveArrayPassedToVarargsMethodNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author eaftan@google.com (Eddie Aftandilian)
             */
            public class PrimitiveArrayPassedToVarargsMethodNegativeCases {

              public void intVarargsMethod(int... ints) {}

              public void intArrayVarargsMethod(int[]... intArrays) {}

              public void objectVarargsMethodWithMultipleParams(Object obj1, Object... objs) {}

              public void doIt() {
                int[] intArray = {1, 2, 3};

                intVarargsMethod(intArray);
                intArrayVarargsMethod(intArray);
                objectVarargsMethodWithMultipleParams(new Object());
              }
            }\
            """)
        .doTest();
  }
}
