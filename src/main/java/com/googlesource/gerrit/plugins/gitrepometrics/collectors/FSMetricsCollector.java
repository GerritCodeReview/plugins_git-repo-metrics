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

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.gitrepometrics.UpdateGitMetricsExecutor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.eclipse.jgit.internal.storage.file.FileRepository;

public class FSMetricsCollector implements MetricsCollector {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static class MetricsRecord {
    private long numberOfKeepFilesCount = 0L;
    private long numberOfEmptyDirectoriesCount = 0L;
    private long numberOfDirectoriesCount = 0L;
    private long numberOfFilesCount = 0L;

    void foundKeepFile() {
      numberOfKeepFilesCount++;
    }

    void foundEmptyDirectory() {
      numberOfEmptyDirectoriesCount++;
    }

    void foundDirectory() {
      numberOfDirectoriesCount++;
    }

    void foundFile() {
      numberOfFilesCount++;
    }

    void incrementMetrics(MetricsRecord metricsRecordInc) {
      numberOfKeepFilesCount += metricsRecordInc.numberOfKeepFilesCount;
      numberOfEmptyDirectoriesCount += metricsRecordInc.numberOfEmptyDirectoriesCount;
      numberOfDirectoriesCount += metricsRecordInc.numberOfDirectoriesCount;
      numberOfFilesCount += metricsRecordInc.numberOfFilesCount;
    }

    HashMap<GitRepoMetric, Long> toMap() {
      HashMap<GitRepoMetric, Long> metrics = new HashMap<>(4);
      metrics.put(numberOfFiles, numberOfFilesCount);
      metrics.put(numberOfDirectories, numberOfDirectoriesCount);
      metrics.put(numberOfEmptyDirectories, numberOfEmptyDirectoriesCount);
      metrics.put(numberOfKeepFiles, numberOfKeepFilesCount);
      return metrics;
    }
  }

  protected static final GitRepoMetric numberOfKeepFiles =
      new GitRepoMetric("numberOfKeepFiles", "Number of keep files on filesystem", "Count");
  protected static final GitRepoMetric numberOfEmptyDirectories =
      new GitRepoMetric(
          "numberOfEmptyDirectories", "Number of empty directories on filesystem", "Count");
  protected static final GitRepoMetric numberOfDirectories =
      new GitRepoMetric("numberOfDirectories", "Number of directories on filesystem", "Count");
  protected static final GitRepoMetric numberOfFiles =
      new GitRepoMetric("numberOfFiles", "Number of directories on filesystem", "Count");

  private static final ImmutableList<GitRepoMetric> availableMetrics =
      ImmutableList.of(
          numberOfKeepFiles, numberOfEmptyDirectories, numberOfFiles, numberOfDirectories);

  private final ExecutorService executorService;

  @Inject
  public FSMetricsCollector(@UpdateGitMetricsExecutor ExecutorService executorService) {
    this.executorService = executorService;
  }

  @Override
  public void collect(
      FileRepository repository,
      String projectName,
      Consumer<HashMap<GitRepoMetric, Long>> populateMetrics) {

    executorService.submit(
        () -> {
          populateMetrics.accept(filesAndDirectoriesCount(repository, projectName));
        });
  }

  private HashMap<GitRepoMetric, Long> filesAndDirectoriesCount(
      FileRepository repository, String projectName) {

    try (Stream<Path> objDir = Files.walk(repository.getObjectsDirectory().toPath())) {
      MetricsRecord metricsRecord =
          objDir
              .map(
                  path -> {
                    File f = path.toFile();
                    MetricsRecord mr = new MetricsRecord();
                    if (f.isFile()) {
                      mr.foundFile();
                      if (f.getName().endsWith(".keep")) {
                        mr.foundKeepFile();
                      }
                    } else {
                      mr.foundDirectory();
                      if (Objects.requireNonNull(f.listFiles()).length == 0) {
                        mr.foundEmptyDirectory();
                      }
                    }
                    return mr;
                  })
              .reduce(
                  new MetricsRecord(),
                  (acc, lastMetric) -> {
                    acc.incrementMetrics(lastMetric);
                    return acc;
                  });
      return metricsRecord.toMap();
    } catch (IOException e) {
      logger.atSevere().withCause(e).log(
          "Error reading from file system for project %s", projectName);
    }

    return new MetricsRecord().toMap();
  }

  @Override
  public String getMetricsCollectorName() {
    return "filesystem-statistics";
  }

  @Override
  public ImmutableList<GitRepoMetric> availableMetrics() {
    return availableMetrics;
  }
}
