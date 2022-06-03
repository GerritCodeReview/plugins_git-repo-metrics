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

package com.googlesource.gerrit.plugins.gitrepometrics;

import static com.googlesource.gerrit.plugins.gitrepometrics.GitRepoMetricsCacheModule.metrics;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitStats;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;

public class UpdateGitMetricsTask implements Runnable {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Repository repository;
  private final Project project;

  UpdateGitMetricsTask(Repository repository, Project project) {
    this.repository = repository;
    this.project = project;
  }

  @Override
  public void run() {
    // TODO Might be a noop
    logger.atInfo().log(
        "Running task to collect stats: repo %s, project %s",
        repository.getIdentifier(), project.getName());
    // TODO Loop through all the collectors
    GitStats gitStats = new GitStats((FileRepository) repository, project);
    Map<String, Long> newMetrics = gitStats.get();
    logger.atInfo().log(
        "Here all the metrics for %s - %s", project.getName(), getStringFromMap(newMetrics));
    metrics.putAll(newMetrics);
  }

  String getStringFromMap(Map<String, Long> m) {
    return m.keySet().stream()
        .map(key -> key + "=" + m.get(key))
        .collect(Collectors.joining(", ", "{", "}"));
  }

  @Override
  public String toString() {
    return String.join(" - ", repository.toString(), project.getName());
  }
}
