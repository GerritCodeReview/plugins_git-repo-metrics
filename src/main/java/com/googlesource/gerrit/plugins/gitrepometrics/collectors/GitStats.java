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
import com.googlesource.gerrit.plugins.gitrepometrics.GitRepoMetricsCache;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.internal.storage.file.GC;

// TODO Add an interface
// TODO implement multiple collectors
public class GitStats {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final FileRepository repository;
  private final Project p;

  public static String numberOfPackedObjects = "numberOfPackedObjects";
  public static String numberOfPackFiles = "numberOfPackFiles";
  public static String numberOfLooseObjects = "numberOfLooseObjects";
  public static String numberOfLooseRefs = "numberOfLooseRefs";
  public static String numberOfPackedRefs = "numberOfPackedRefs";
  public static String sizeOfLooseObjects = "sizeOfLooseObjects";
  public static String sizeOfPackedObjects = "sizeOfPackedObjects";
  public static String numberOfBitmaps = "numberOfBitmaps";

  public GitStats(FileRepository repository, Project project) {
    this.repository = repository;
    this.p = project;
  }

  public Map<String, Long> get() {
    Map<String, Long> metrics = new java.util.HashMap<>(Collections.emptyMap());
    try {
      GC.RepoStatistics statistics = new GC(repository).getStatistics();
      putMetric(metrics, numberOfPackedObjects, s -> s.numberOfPackedObjects, statistics);
      putMetric(metrics, numberOfPackFiles, s -> s.numberOfPackFiles, statistics);
      putMetric(metrics, numberOfLooseObjects, s -> s.numberOfLooseObjects, statistics);
      putMetric(metrics, numberOfLooseRefs, s -> s.numberOfLooseRefs, statistics);
      putMetric(metrics, numberOfPackedRefs, s -> s.numberOfPackedRefs, statistics);
      putMetric(metrics, sizeOfLooseObjects, s -> s.sizeOfLooseObjects, statistics);
      putMetric(metrics, sizeOfPackedObjects, s -> s.sizeOfPackedObjects, statistics);
      putMetric(metrics, numberOfBitmaps, s -> s.numberOfBitmaps, statistics);
      logger.atInfo().log("New Git Statistics metrics collected: %s", statistics.toString());
    } catch (IOException e) {
      logger.atSevere().log("Something went wrong: %s", e.getMessage());
    }
    return metrics;
  }

  public static List<GitRepoMetric> availableMetrics() {
    return Arrays.asList(
        new GitRepoMetric(numberOfPackedObjects, "Number of packed objects", "Count"),
        new GitRepoMetric(numberOfPackFiles, "Number of pack files", "Count"),
        new GitRepoMetric(numberOfLooseObjects, "Number of loose objects", "Count"),
        new GitRepoMetric(numberOfLooseRefs, "Number of loose refs", "Count"),
        new GitRepoMetric(numberOfPackedRefs, "Number of packed refs", "Count"),
        new GitRepoMetric(sizeOfLooseObjects, "Size of loose objects", "Count"),
        new GitRepoMetric(sizeOfPackedObjects, "Size of packed objects", "Count"),
        new GitRepoMetric(numberOfBitmaps, "Number of bitmaps", "Count"));
  }

  private void putMetric(
      Map<String, Long> metrics,
      String metricName,
      Function<GC.RepoStatistics, Long> fn,
      GC.RepoStatistics statistics) {
    metrics.put(GitRepoMetricsCache.getMetricName(metricName, p.getName()), fn.apply(statistics));
  }

  public String getStatName() {
    return "git-statistics";
  }
}
