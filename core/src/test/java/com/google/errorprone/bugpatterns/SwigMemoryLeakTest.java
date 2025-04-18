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

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author irogers@google.com (Ian Rogers)
 */
@RunWith(JUnit4.class)
public class SwigMemoryLeakTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(SwigMemoryLeak.class, getClass());

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "SwigMemoryLeakPositiveCases.java",
"""
package com.google.errorprone.bugpatterns.testdata;

/**
 * @author irogers@google.com (Ian Rogers)
 */
public class SwigMemoryLeakPositiveCases {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  public SwigMemoryLeakPositiveCases(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        // BUG: Diagnostic contains: SWIG generated code that can't call a C++ destructor will leak
        // memory
        throw new UnsupportedOperationException("C++ destructor does not have public access");
      }
      swigCPtr = 0;
    }
  }
}\
""")
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "SwigMemoryLeakNegativeCases.java",
            """
            package com.google.errorprone.bugpatterns.testdata;

            /**
             * @author irogers@google.com (Ian Rogers)
             */
            public class SwigMemoryLeakNegativeCases {
              private long swigCPtr;
              protected boolean swigCMemOwn;

              public SwigMemoryLeakNegativeCases(long cPtr, boolean cMemoryOwn) {
                swigCMemOwn = cMemoryOwn;
                swigCPtr = cPtr;
              }

              @SuppressWarnings("removal") // deprecated for removal starting in JDK 18
              protected void finalize() {
                delete();
              }

              public synchronized void delete() {
                if (swigCPtr != 0) {
                  if (swigCMemOwn) {
                    swigCMemOwn = false;
                    nativeDelete(swigCPtr);
                  }
                  swigCPtr = 0;
                }
              }

              private static native void nativeDelete(long cptr);
            }\
            """)
        .doTest();
  }
}
