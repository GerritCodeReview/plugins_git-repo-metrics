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
import java.util.concurrent.atomic.AtomicInteger;
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
  public HashMap<GitRepoMetric, Long> collect(FileRepository repository, Project project) {
    HashMap<GitRepoMetric, Long> metrics = new HashMap<>();

    HashMap<String, Long> metricsV = new HashMap<>();
    HashMap<String, AtomicInteger> partialMetrics =
        filesAndDirectoriesCount(repository, project, metricsV);

    metrics.put(
        numberOfEmptyDirectories,
        partialMetrics.get(numberOfEmptyDirectories.getName()).longValue());
    metrics.put(numberOfDirectories, partialMetrics.get(numberOfDirectories.getName()).longValue());
    metrics.put(numberOfFiles, partialMetrics.get(numberOfFiles.getName()).longValue());
    metrics.put(numberOfKeepFiles, partialMetrics.get(numberOfKeepFiles.getName()).longValue());

    return metrics;
  }

  private HashMap<String, AtomicInteger> filesAndDirectoriesCount(
      FileRepository repository, Project project, HashMap<String, Long> metrics) {
    HashMap<String, AtomicInteger> counter =
        new HashMap<String, AtomicInteger>() {
          {
            put(numberOfFiles.getName(), new AtomicInteger(0));
            put(numberOfDirectories.getName(), new AtomicInteger(0));
            put(numberOfEmptyDirectories.getName(), new AtomicInteger(0));
            put(numberOfKeepFiles.getName(), new AtomicInteger(0));
          }
        };
    try {
      Files.walk(repository.getObjectsDirectory().toPath())
          .parallel()
          .forEach(
              path -> {
                if (path.toFile().isFile()) {
                  counter
                      .get(numberOfFiles.getName())
                      .updateAndGet(metricCounter -> metricCounter + 1);

                  if (path.toFile().getName().endsWith(".keep")) {
                    counter
                        .get(numberOfKeepFiles.getName())
                        .updateAndGet(metricCounter -> metricCounter + 1);
                  }
                }
                if (path.toFile().isDirectory()) {
                  counter
                      .get(numberOfDirectories.getName())
                      .updateAndGet(metricCounter -> metricCounter + 1);
                  if (Objects.requireNonNull(path.toFile().listFiles()).length == 0) {
                    counter
                        .get(numberOfEmptyDirectories.getName())
                        .updateAndGet(metricCounter -> metricCounter + 1);
                  }
                }
              });
    } catch (IOException e) {
      logger.atSevere().withCause(e).log(
          "Can't open object directory for project " + project.getName());
    }

    return counter;
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
