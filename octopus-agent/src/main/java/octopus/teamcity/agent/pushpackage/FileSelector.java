/*
 * Copyright (c) Octopus Deploy and contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 *  these files except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package octopus.teamcity.agent.pushpackage;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileSelector {
  private static final Logger LOG = LogManager.getLogger();

  public static List<File> getMatchingFiles(final Path rootPath, final List<String> globs) {
    final List<File> result = Lists.newArrayList();
    globs.forEach(
        entry -> {
          final Path fullPath = rootPath.resolve(entry);
          final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + fullPath);
          try {
            Files.walkFileTree(
                rootPath,
                new SimpleFileVisitor<Path>() {

                  @Override
                  public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                      throws IOException {
                    if (matcher.matches(file)) {
                      result.add(file.toFile());
                    }
                    return super.visitFile(file, attrs);
                  }
                });
          } catch (IOException e) {
            LOG.error("Failed to walk tree when finding files");
          }
        });
    return result;
  }
}
