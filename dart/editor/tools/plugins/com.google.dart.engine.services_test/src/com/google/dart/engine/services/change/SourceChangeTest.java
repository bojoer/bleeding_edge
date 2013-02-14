/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.engine.services.change;

import com.google.dart.engine.formatter.edit.Edit;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.source.Source;

import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SourceChangeTest extends TestCase {
  private final Source source = mock(Source.class);

  public void test_access() throws Exception {
    SourceChange change = new SourceChange("myName", source);
    assertEquals("myName", change.getName());
    assertSame(source, change.getSource());
  }

  public void test_edits() throws Exception {
    Edit editA = mock(Edit.class);
    Edit editB = mock(Edit.class);
    // empty
    SourceChange change = new SourceChange("test", source);
    assertThat(change.getEdits()).isEmpty();
    // add edits
    change.addEdit(editA);
    change.addEdit(editB);
    assertThat(change.getEdits()).containsExactly(editA, editB);
  }
}