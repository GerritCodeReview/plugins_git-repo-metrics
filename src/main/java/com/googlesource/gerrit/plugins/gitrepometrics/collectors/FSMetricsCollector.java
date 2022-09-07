// Copyright (C) 2022 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.gitrepometrics.collectors;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;


import org.eclipse.jgit.internal.storage.file.FileRepository;

public class FSMetricsCollector implements MetricsCollector {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  protected static final GitRepoMetric numberOfKeepFiles =
      new GitRepoMetric("numberOfKeepFiles", "Number of keep files on filesystem", "Count");
  protected static final GitRepoMetric numberOfEmptyDirectories =
      new GitRepoMetric(
          "numberOfEmptyDirectories", "Number of empty directories on filesystem", "Count");
  protected static final GitRepoMetric numberOfDirectories =
      new GitRepoMetric("numberOfDirectories", "Number of directories on filesystem", "Count");
  protected static final GitRepoMetric numberOfFiles =
      new GitRepoMetric("numberOfFiles", "Number of directories on filesystem", "Count");

  @Override
  public void collect(FileRepository repository, Project project, ExecutorService executorService, Consumer <HashMap <GitRepoMetric, Long>> populateMetrics) {

    filesAndDirectoriesCount(repository, project, executorService, populateMetrics);
  }

  private void filesAndDirectoriesCount(
      FileRepository repository, Project project, ExecutorService executorService, Consumer <HashMap<GitRepoMetric, Long>> populateMetrics) {

      executorService.submit(new Callable <HashMap < GitRepoMetric, Long >>() {
        @Override
        public HashMap <GitRepoMetric, Long> call() throws Exception {
          HashMap < GitRepoMetric, Long > r = new HashMap <GitRepoMetric, Long>();
          try {
            r = Files.walk(repository.getObjectsDirectory().toPath())
                    .collect(Collectors.toList())
                    .parallelStream()
                    .map(path -> {
                      HashMap <GitRepoMetric, Long> c = new HashMap <GitRepoMetric, Long>() {
                        {
                          put(numberOfFiles, 0L);
                          put(numberOfDirectories, 0L);
                          put(numberOfEmptyDirectories, 0L);
                          put(numberOfKeepFiles, 0L);
                        }
                      };
                      if (path.toFile().isFile()) {
                        c.put(numberOfFiles, 1L);
                        if (path.toFile().getName().endsWith(".keep")) {
                          c.put(numberOfKeepFiles, 1L);
                        }
                      }
                      if (path.toFile().isDirectory()) {
                        c.put(numberOfDirectories, 1L);
                        if (Objects.requireNonNull(path.toFile().listFiles()).length == 0) {
                          c.put(numberOfEmptyDirectories, 1L);
                        }
                      }
                      return c;
                    }).reduce(new HashMap <GitRepoMetric, Long>(), (a, b) -> new HashMap <GitRepoMetric, Long>() {{
                      put(numberOfFiles, a.get(numberOfFiles) + b.get(numberOfFiles));
                      put(numberOfDirectories., a.get(numberOfDirectories) + b.get(numberOfDirectories));
                      put(numberOfEmptyDirectories, a.get(numberOfEmptyDirectories) + b.get(numberOfEmptyDirectories));
                      put(numberOfKeepFiles, a.get(numberOfKeepFiles) + b.get(numberOfKeepFiles));
                    }});
          } catch (IOException e) {
            logger.atSevere().withCause(e).log(
                    "Can't open object directory for project " + project.getName());
          }

          populateMetrics.accept(r);
          return r;
        }
      });

  }



  @Override
  public String getMetricsCollectorName() {
    return "filesystem-statistics";
  }

  @Override
  public List<GitRepoMetric> availableMetrics() {
    return Arrays.asList(
        numberOfKeepFiles, numberOfEmptyDirectories, numberOfFiles, numberOfDirectories);
  }
}
