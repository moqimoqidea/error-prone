/*
 * Copyright 2015 The Error Prone Authors.
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
 * @author chy@google.com (Christine Yang)
 * @author kmuhlrad@google.com (Katy Muhlrad)
 */
@RunWith(JUnit4.class)
public class GetClassOnClassTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(GetClassOnClass.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "GetClassOnClassPositiveCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author chy@google.com (Christine Yang)
             * @author kmuhlrad@google.com (Katy Muhlrad)
             */
            public class GetClassOnClassPositiveCases {

              public void getClassOnClass(Class clazz) {
                // BUG: Diagnostic contains: clazz.getName()
                System.out.println(clazz.getClass().getName());
              }

              public void getClassOnClass2() {
                String s = "hi";
                // BUG: Diagnostic contains: s.getClass().getName()
                s.getClass().getClass().getName();
              }

              public void getClassOnClass3() {
                // BUG: Diagnostic contains: String.class.getName()
                System.out.println(String.class.getClass().getName());
              }
            }\
            """)
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "GetClassOnClassNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author chy@google.com (Christine Yang)
             * @author kmuhlrad@google.com (Katy Muhlrad)
             */
            public class GetClassOnClassNegativeCases {

              public void getClassOnClass(Object obj) {
                System.out.println(obj.getClass().getName());
              }

              public void getClassOnClass2() {
                String s = "hi";
                DummyObject.getClass(s);
              }

              public static class DummyObject {
                public static boolean getClass(Object a) {
                  return true;
                }
              }
            }\
            """)
        .doTest();
  }
}
