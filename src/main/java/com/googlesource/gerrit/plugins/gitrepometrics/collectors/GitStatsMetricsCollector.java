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
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.internal.storage.file.GC;

public class GitStatsMetricsCollector implements MetricsCollector {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static final GitRepoMetric numberOfPackedObjects =
      new GitRepoMetric("numberOfPackedObjects", "Number of packed objects", "Count");
  public static final GitRepoMetric numberOfPackFiles =
      new GitRepoMetric("numberOfPackFiles", "Number of pack files", "Count");
  public static final GitRepoMetric numberOfLooseObjects =
      new GitRepoMetric("numberOfLooseObjects", "Number of loose objects", "Count");
  public static final GitRepoMetric numberOfLooseRefs =
      new GitRepoMetric("numberOfLooseRefs", "Number of loose refs", "Count");
  public static final GitRepoMetric numberOfPackedRefs =
      new GitRepoMetric("numberOfPackedRefs", "Number of packed refs", "Count");
  public static final GitRepoMetric sizeOfLooseObjects =
      new GitRepoMetric("sizeOfLooseObjects", "Size of loose objects", "Count");
  public static final GitRepoMetric sizeOfPackedObjects =
      new GitRepoMetric("sizeOfPackedObjects", "Size of packed objects", "Count");
  public static final GitRepoMetric numberOfBitmaps =
      new GitRepoMetric("numberOfBitmaps", "Number of bitmaps", "Count");

  private static final ImmutableList<GitRepoMetric> availableMetrics =
      ImmutableList.of(
          numberOfPackedObjects,
          numberOfPackFiles,
          numberOfLooseObjects,
          numberOfLooseRefs,
          numberOfPackedRefs,
          sizeOfLooseObjects,
          sizeOfPackedObjects,
          numberOfBitmaps);

  private final ExecutorService executorService;

  @Inject
  GitStatsMetricsCollector(@UpdateGitMetricsExecutor ExecutorService executorService) {
    this.executorService = executorService;
  }

  @Override
  public void collect(
      FileRepository repository,
      String projectName,
      Consumer<HashMap<GitRepoMetric, Long>> populateMetrics) {

    executorService.submit(
        () -> {
          try {
            Thread.sleep(5000);
          } catch (Exception e) {
            /// nothing
          }
          HashMap<GitRepoMetric, Long> metrics = new HashMap<>();
          try {
            GC.RepoStatistics statistics = new GC(repository).getStatistics();
            metrics.put(numberOfPackedObjects, statistics.numberOfPackedObjects);
            metrics.put(numberOfPackFiles, statistics.numberOfPackFiles);
            metrics.put(numberOfLooseObjects, statistics.numberOfLooseObjects);
            metrics.put(numberOfLooseRefs, statistics.numberOfLooseRefs);
            metrics.put(numberOfPackedRefs, statistics.numberOfPackedRefs);
            metrics.put(sizeOfLooseObjects, statistics.sizeOfLooseObjects);
            metrics.put(sizeOfPackedObjects, statistics.sizeOfPackedObjects);
            metrics.put(numberOfBitmaps, statistics.numberOfBitmaps);
            logger.atInfo().log("New Git Statistics metrics collected: %s", statistics.toString());
          } catch (IOException e) {
            logger.atSevere().log("Something went wrong: %s", e.getMessage());
          }

          populateMetrics.accept(metrics);
        });
  }

  @Override
  public ImmutableList<GitRepoMetric> availableMetrics() {
    return availableMetrics;
  }

  @Override
  public String getMetricsCollectorName() {
    return "git-statistics";
  }
}
