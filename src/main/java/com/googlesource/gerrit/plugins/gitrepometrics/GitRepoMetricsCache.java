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

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitRepoMetric;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.MetricsCollector;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GitRepoMetricsCache {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private Map<String, Long> metrics;
  private final MetricMaker metricMaker;
  private final MetricRegistry metricRegistry;
  private final Set<String> projects;
  private final boolean collectAllRepositories;
  private ImmutableList<GitRepoMetric> metricsNames;
  private DynamicSet<MetricsCollector> collectors;
  private Set<String> staleStatsProjects;

  @Inject
  GitRepoMetricsCache(
      DynamicSet<MetricsCollector> collectors,
      MetricMaker metricMaker,
      MetricRegistry metricRegistry,
      GitRepoMetricsConfig config) {
    this.collectors = collectors;
    this.metricMaker = metricMaker;
    this.metricRegistry = metricRegistry;

    this.metricsNames =
        collectors.stream()
            .flatMap(c -> c.availableMetrics().stream())
            .collect(collectingAndThen(toList(), ImmutableList::copyOf));

    this.projects = new HashSet<>(config.getRepositoryNames());
    this.metrics = Maps.newHashMap();
    this.collectAllRepositories = config.collectAllRepositories();
    this.staleStatsProjects = ConcurrentHashMap.newKeySet();
  }

  public Map<String, Long> getMetrics() {
    return metrics;
  }

  public void setMetrics(Map<GitRepoMetric, Long> newMetrics, String projectName) {
    newMetrics.forEach(
        (repoMetric, value) -> {
          String metricsName = getMetricName(repoMetric.getName(), projectName);
          logger.atFine().log(
              "Collected %s for project %s: %d", repoMetric.getName(), projectName, value);
          metrics.put(metricsName, value);

          if (!metricExists(metricsName)) {
            createNewCallbackMetric(repoMetric, projectName);
          }
        });
  }

  private boolean metricExists(String metricName) {
    return metricRegistry
        .getMetrics()
        .containsKey(String.format("%s/%s/%s", "plugins", "git-repo-metrics", metricName));
  }

  private void createNewCallbackMetric(GitRepoMetric metric, String projectName) {
    String metricName = getMetricName(metric.getName(), projectName);
    CallbackMetric0<Long> cb =
        metricMaker.newCallbackMetric(
            metricName,
            Long.class,
            new Description(metric.getDescription()).setRate().setUnit(metric.getUnit()));

    metricMaker.newTrigger(cb, () -> cb.set(getMetrics().getOrDefault(metricName, 0L)));
  }

  public List<GitRepoMetric> getMetricsNames() {
    return metricsNames;
  }

  public DynamicSet<MetricsCollector> getCollectors() {
    return collectors;
  }

  public static String getMetricName(String metricName, String projectName) {
    return String.format("%s_%s", metricName, projectName).toLowerCase(Locale.ROOT);
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
