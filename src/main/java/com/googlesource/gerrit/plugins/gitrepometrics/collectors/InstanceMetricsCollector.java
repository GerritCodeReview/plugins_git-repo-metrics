// Copyright (C) 2024 The Android Open Source Project
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

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.index.project.ProjectData;
import com.google.gerrit.index.project.ProjectIndexCollection;
import com.google.gerrit.index.query.Predicate;
import com.google.gerrit.index.query.QueryParseException;
import com.google.gerrit.index.query.QueryResult;
import com.google.gerrit.server.query.project.ProjectQueryProcessor;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.gitrepometrics.UpdateGitMetricsExecutor;

public class InstanceMetricsCollector implements GenericMetricsCollector {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static final GitRepoMetric numberOfProjects =
      new GitRepoMetric("numberOfProjects", "Number of projects", "Count");

  private static final ImmutableList<GitRepoMetric> availableMetrics =
      ImmutableList.of(numberOfProjects);

  private final ExecutorService executorService;
  private final Provider<ProjectQueryProcessor> projectQueryProcessorProvider;

  @Inject
  public InstanceMetricsCollector(
      @UpdateGitMetricsExecutor ExecutorService executorService,
      Provider<ProjectQueryProcessor> projectQueryProcessorProvider) {
    this.executorService = executorService;
    this.projectQueryProcessorProvider = projectQueryProcessorProvider;
  }

  @Override
  public void collect(Consumer<HashMap<GitRepoMetric, Long>> populateMetrics) {
    executorService.submit(
        () -> {
          HashMap<GitRepoMetric, Long> metrics = new HashMap<>();

          long repoCount = 0;

          ProjectQueryProcessor queryProcessor = projectQueryProcessorProvider.get();

          try {
            QueryResult<ProjectData> result = queryProcessor.query(Predicate.any());

            repoCount = result.entities().size();
          } catch (QueryParseException e) {
            logger.atSevere().withCause(e).log("Error querying projects");
          }

          metrics.put(numberOfProjects, repoCount);

          logger.atInfo().log("Repo Count metric collected: %d", repoCount);

          populateMetrics.accept(metrics);
        });
  }

  @Override
  public ImmutableList<GitRepoMetric> availableMetrics() {
    return availableMetrics;
  }

  @Override
  public String getMetricsCollectorName() {
    return "instance-statistics";
  }
}
