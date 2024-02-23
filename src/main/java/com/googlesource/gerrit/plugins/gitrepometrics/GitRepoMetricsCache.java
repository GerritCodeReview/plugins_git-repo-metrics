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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.metrics.CallbackMetric0;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GenericMetricsCollector;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitRepoMetric;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.MetricsCollector;
import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GitRepoMetricsCache {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String GENERIC_METRICS =
      "#GENERIC#"; // So that there are no collisions with any project name

  private Map<String, Long> metrics;
  private final MetricMaker metricMaker;
  private final MetricRegistry metricRegistry;
  private final Set<String> projects;
  private Map<String, Long> collectedAt;
  private final long gracePeriodMs;
  private ImmutableList<GitRepoMetric> metricsNames;
  private DynamicSet<MetricsCollector> collectors;
  DynamicSet<GenericMetricsCollector> genericCollectors;

  private final Clock clock;

  @VisibleForTesting
  GitRepoMetricsCache(
      DynamicSet<MetricsCollector> collectors,
      DynamicSet<GenericMetricsCollector> genericCollectors,
      MetricMaker metricMaker,
      MetricRegistry metricRegistry,
      GitRepoMetricsConfig config,
      Clock clock) {
    this.collectors = collectors;
    this.genericCollectors = genericCollectors;

    this.metricMaker = metricMaker;
    this.metricRegistry = metricRegistry;

    this.metricsNames =
        collectors.stream()
            .flatMap(c -> c.availableMetrics().stream())
            .collect(collectingAndThen(toList(), ImmutableList::copyOf));

    this.projects = new HashSet<>(config.getRepositoryNames());
    this.metrics = Maps.newHashMap();
    this.collectedAt = Maps.newHashMap();
    this.clock = clock;
    this.gracePeriodMs = config.getGracePeriodMs();
  }

  @Inject
  GitRepoMetricsCache(
      DynamicSet<MetricsCollector> collectors,
      DynamicSet<GenericMetricsCollector> genericCollectors,
      MetricMaker metricMaker,
      MetricRegistry metricRegistry,
      GitRepoMetricsConfig config) {
    this(
        collectors,
        genericCollectors,
        metricMaker,
        metricRegistry,
        config,
        Clock.systemDefaultZone());
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
    collectedAt.put(projectName, clock.millis());
  }

  public void setMetrics(Map<GitRepoMetric, Long> newMetrics) {
    newMetrics.forEach(
        (repoMetric, value) -> {
          String metricsName = getMetricName(repoMetric.getName());
          logger.atFine().log("Collected %s: %d", repoMetric.getName(), value);
          metrics.put(metricsName, value);

          if (!metricExists(metricsName)) {
            createNewCallbackMetric(repoMetric);
          }
        });
    collectedAt.put(GENERIC_METRICS, clock.millis());
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

  private void createNewCallbackMetric(GitRepoMetric metric) {
    String metricName = getMetricName(metric.getName(), null);
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

  public DynamicSet<MetricsCollector> getProjectCollectors() {
    return collectors;
  }

  public DynamicSet<GenericMetricsCollector> getGenericCollectors() {
    return genericCollectors;
  }

  @VisibleForTesting
  public Map<String, Long> getCollectedAt() {
    return collectedAt;
  }

  @VisibleForTesting
  public Long getGenericCollectedAt() {
    return collectedAt.get(GENERIC_METRICS);
  }

  public static String getMetricName(String metricName, String projectName) {
    return String.format("%s_%s", metricName, projectName).toLowerCase(Locale.ROOT);
  }

  public static String getMetricName(String metricName) {
    return metricName.toLowerCase(Locale.ROOT);
  }

  public boolean shouldCollectStats(String projectName) {
    long lastCollectionTime = collectedAt.getOrDefault(projectName, 0L);
    long currentTimeMs = System.currentTimeMillis();
    boolean doCollectStats = lastCollectionTime + gracePeriodMs <= currentTimeMs;
    if (!doCollectStats) {
      logger.atFine().log(
          "Skip stats collection for %s (grace period: %d, last collection time: %d, current time: %d",
          projectName, gracePeriodMs, lastCollectionTime, currentTimeMs);
      return false;
    }

    return projects.contains(projectName);
  }
}
