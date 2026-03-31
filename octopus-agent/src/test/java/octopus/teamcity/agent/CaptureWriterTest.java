/*
 * Copyright 2000-2012 Octopus Deploy Pty. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package octopus.teamcity.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class CaptureWriterTest {

  @Test
  void delegatesToUnderlyingWriter() {
    List<String> delegateOutput = new ArrayList<>();
    CaptureWriter writer = new CaptureWriter(delegateOutput::add);

    writer.write("hello");
    writer.write("world");

    assertThat(delegateOutput).containsExactly("hello", "world");
  }

  @Test
  void capturesAllLines() {
    CaptureWriter writer = new CaptureWriter(text -> {});

    writer.write("line one");
    writer.write("line two");
    writer.write("line three");

    assertThat(writer.getCapturedOutput()).isEqualTo("line one\nline two\nline three\n");
  }

  @Test
  void emptyOutputReturnsEmptyString() {
    CaptureWriter writer = new CaptureWriter(text -> {});
    assertThat(writer.getCapturedOutput()).isEmpty();
  }
}
