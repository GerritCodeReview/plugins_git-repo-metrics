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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.internal.storage.file.GC;

public class GitStats implements MetricsCollector {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static String numberOfPackedObjects = "numberOfPackedObjects";
  public static String numberOfPackFiles = "numberOfPackFiles";
  public static String numberOfLooseObjects = "numberOfLooseObjects";
  public static String numberOfLooseRefs = "numberOfLooseRefs";
  public static String numberOfPackedRefs = "numberOfPackedRefs";
  public static String sizeOfLooseObjects = "sizeOfLooseObjects";
  public static String sizeOfPackedObjects = "sizeOfPackedObjects";
  public static String numberOfBitmaps = "numberOfBitmaps";

  @Override
  public HashMap<String, Long> collect(FileRepository repository, Project project) {
    HashMap<String, Long> metrics = new HashMap<>(availableMetrics().size());
    try {
      GC.RepoStatistics statistics = new GC(repository).getStatistics();
      putMetric(project, metrics, numberOfPackedObjects, statistics.numberOfPackedObjects);
      putMetric(project, metrics, numberOfPackFiles, statistics.numberOfPackFiles);
      putMetric(project, metrics, numberOfLooseObjects, statistics.numberOfLooseObjects);
      putMetric(project, metrics, numberOfLooseRefs, statistics.numberOfLooseRefs);
      putMetric(project, metrics, numberOfPackedRefs, statistics.numberOfPackedRefs);
      putMetric(project, metrics, sizeOfLooseObjects, statistics.sizeOfLooseObjects);
      putMetric(project, metrics, sizeOfPackedObjects, statistics.sizeOfPackedObjects);
      putMetric(project, metrics, numberOfBitmaps, statistics.numberOfBitmaps);
      logger.atInfo().log("New Git Statistics metrics collected: %s", statistics.toString());
    } catch (IOException e) {
      logger.atSevere().log("Something went wrong: %s", e.getMessage());
    }
    return metrics;
  }

  @Override
  public List<GitRepoMetric> availableMetrics() {
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

  @Override
  public String getMetricsCollectorName() {
    return "git-statistics";
  }
}
