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
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.metrics.CallbackMetric1;
import com.google.gerrit.metrics.Description;
import com.google.gerrit.metrics.MetricMaker;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitRepoMetric;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.MetricsCollector;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
          createNewCallbackMetric(repoMetric, projectName);
        });
  }

  private boolean metricExists(String metricName) {
    return metricRegistry
        .getMetrics()
        .containsKey(String.format("%s/%s/%s", "plugins", "git-repo-metrics", metricName));
  }

  private void createNewCallbackMetric(GitRepoMetric metric, String projectName) {
    CallbackMetric1<String, Long> cb =
        metricMaker.newCallbackMetric(
            metric.getName(),
            Long.class,
            new Description(metric.getDescription()).setRate().setUnit(metric.getUnit()),
            ofProjectName("project_name").description("The name of the project.").build());
    metricMaker.newTrigger(
        cb,
        () -> {
          String metricName = getMetricName(metric.getName(), projectName);
          cb.set(projectName, metrics.get(metricName));
          cb.prune();
        });
  }

  public List<GitRepoMetric> getMetricsNames() {
    return metricsNames;
  }

  public DynamicSet<MetricsCollector> getCollectors() {
    return collectors;
  }

  public static String getMetricName(String metricName, String projectName) {
    return String.format("%s/%s", metricName, sanitizeProjectName(projectName));
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

  private static final Pattern SUBMETRIC_NAME_PATTERN =
      Pattern.compile("[a-zA-Z0-9_-]+([a-zA-Z0-9_-]+)*");
  private static final Pattern INVALID_CHAR_PATTERN = Pattern.compile("[^\\w-]");
  private static final String REPLACEMENT_PREFIX = "_0x";

  /**
   * This logic has been borrowed from:
   * https://gerrit.googlesource.com/gerrit/+/refs/heads/stable-3.10/java/com/google/gerrit/metrics/Field.java#163
   */
  private static String sanitizeProjectName(String projectName) {
    if (SUBMETRIC_NAME_PATTERN.matcher(projectName).matches()
        && !projectName.contains(REPLACEMENT_PREFIX)) {
      return projectName;
    }

    String replacmentPrefixSanitizedName =
        projectName.replaceAll(REPLACEMENT_PREFIX, REPLACEMENT_PREFIX + REPLACEMENT_PREFIX);
    StringBuilder sanitizedName = new StringBuilder();
    for (int i = 0; i < replacmentPrefixSanitizedName.length(); i++) {
      Character c = replacmentPrefixSanitizedName.charAt(i);
      Matcher matcher = INVALID_CHAR_PATTERN.matcher(c.toString());
      if (matcher.matches()) {
        sanitizedName.append(REPLACEMENT_PREFIX);
        sanitizedName.append(Integer.toHexString(c).toUpperCase());
        sanitizedName.append('_');
      } else {
        sanitizedName.append(c);
      }
    }

    return sanitizedName.toString();
  }
}
