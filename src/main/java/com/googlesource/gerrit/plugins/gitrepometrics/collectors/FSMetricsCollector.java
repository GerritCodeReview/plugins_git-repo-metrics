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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
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

  public static String numberOfKeepFiles = "numberOfKeepFiles";
  public static String numberOfEmptyDirectories = "numberOfEmptyDirectories";
  public static String numberOfDirectories = "numberOfDirectories";
  public static String numberOfFiles = "numberOfFiles";

  @VisibleForTesting
  public FSMetricsCollector() {}

  @Override
  public HashMap<String, Long> collect(FileRepository repository, Project project) {
    HashMap<String, Long> metrics = new HashMap<>();

    HashMap<String, AtomicInteger> partialMetrics =
        filesAndDirectoriesCount(repository, project, metrics);
    putMetric(
        project,
        metrics,
        numberOfEmptyDirectories,
        partialMetrics.get(numberOfEmptyDirectories).longValue());
    putMetric(
        project, metrics, numberOfDirectories, partialMetrics.get(numberOfDirectories).longValue());
    putMetric(project, metrics, numberOfFiles, partialMetrics.get(numberOfFiles).longValue());
    putMetric(
        project, metrics, numberOfKeepFiles, partialMetrics.get(numberOfKeepFiles).longValue());

    return metrics;
  }

  private HashMap<String, AtomicInteger> filesAndDirectoriesCount(
      FileRepository repository, Project project, HashMap<String, Long> metrics) {
    HashMap<String, AtomicInteger> counter =
        new HashMap<String, AtomicInteger>() {
          {
            put(numberOfFiles, new AtomicInteger(0));
            put(numberOfDirectories, new AtomicInteger(0));
            put(numberOfEmptyDirectories, new AtomicInteger(0));
            put(numberOfKeepFiles, new AtomicInteger(0));
          }
        };
    try {
      Files.walk(repository.getObjectsDirectory().toPath())
          .parallel()
          .forEach(
              path -> {
                if (path.toFile().isFile()) {
                  counter.get(numberOfFiles).updateAndGet(metricCounter -> metricCounter + 1);
                  if (path.toFile().getName().endsWith(".keep")) {
                    counter.get(numberOfKeepFiles).updateAndGet(metricCounter -> metricCounter + 1);
                  }
                }
                if (path.toFile().isDirectory()) {
                  counter.get(numberOfDirectories).updateAndGet(metricCounter -> metricCounter + 1);
                  if (Objects.requireNonNull(path.toFile().listFiles()).length == 0) {
                    counter
                        .get(numberOfEmptyDirectories)
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
  public ImmutableList <GitRepoMetric> availableMetrics() {
    return ImmutableList.of(
        new GitRepoMetric(numberOfKeepFiles, "Number of keep files on filesystem", "Count"),
        new GitRepoMetric(
            numberOfEmptyDirectories, "Number of empty directories on filesystem", "Count"),
        new GitRepoMetric(numberOfDirectories, "Number of directories on filesystem", "Count"),
        new GitRepoMetric(numberOfFiles, "Number of directories on filesystem", "Count"));
  }
}
