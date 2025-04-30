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

import static com.google.gerrit.metrics.Field.ofProjectName;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.metrics.CallbackMetric1;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitRepoMetric;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.MetricsCollector;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GitRepoMetricsCache {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> metrics;
  private final MetricMaker metricMaker;
  private final Set<String> projects;
  private final boolean collectAllRepositories;
  private final MetricsTracker metricsTracker;
  private final DynamicSet<MetricsCollector> collectors;
  private final Set<String> staleStatsProjects;

  @Inject
  GitRepoMetricsCache(
      DynamicSet<MetricsCollector> collectors,
      MetricMaker metricMaker,
      MetricsTracker metricsTracker,
      GitRepoMetricsConfig config) {
    this.collectors = collectors;
    this.metricMaker = metricMaker;
    this.metricsTracker = metricsTracker;
    this.projects = new HashSet<>(config.getRepositoryNames());
    this.metrics = new ConcurrentHashMap<>();
    this.collectAllRepositories = config.collectAllRepositories();
    this.staleStatsProjects = ConcurrentHashMap.newKeySet();
  }

  @VisibleForTesting
  public ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> getMetrics() {
    return metrics;
  }

  public void setMetrics(Map<GitRepoMetric, Long> newMetrics, String projectName) {
    newMetrics.forEach(
        (repoMetric, value) -> {
          String metricsName = repoMetric.getName().toLowerCase(Locale.ROOT);
          metrics
              .computeIfAbsent(metricsName, (m) -> new ConcurrentHashMap<>())
              .put(projectName.toLowerCase(Locale.ROOT), value);
          if (!metricsTracker.metricExists(metricsName)) {
            createNewCallbackMetric(repoMetric);
          }
        });
  }

  private void createNewCallbackMetric(GitRepoMetric metric) {
    String metricName = metric.getName();
    CallbackMetric1<String, Long> cb =
        metricMaker.newCallbackMetric(
            metricName.toLowerCase(Locale.ROOT),
            Long.class,
            new Description(metric.getDescription()).setRate().setUnit(metric.getUnit()),
            ofProjectName("project_name").description("The name of the project.").build());
    metricMaker.newTrigger(
        cb,
        () -> {
          Map<String, Long> projectsMetrics = metrics.get(metricName.toLowerCase(Locale.ROOT));
          if (projectsMetrics == null || projectsMetrics.isEmpty()) {
            throw new IllegalStateException(
                "Unexpected empty project metrics when populating '" + metricName + "'");
          } else {
            projectsMetrics.forEach(cb::set);
            cb.prune();
          }
        });
  }

  public DynamicSet<MetricsCollector> getCollectors() {
    return collectors;
  }

  public boolean shouldCollectStats(String projectName) {
    return (collectAllRepositories || projects.contains(projectName))
        && !staleStatsProjects.contains(projectName);
  }

  public void setStale(String projectName) {
    staleStatsProjects.add(projectName);
  }

  public void unsetStale(String projectName) {
    staleStatsProjects.remove(projectName);
  }
}
