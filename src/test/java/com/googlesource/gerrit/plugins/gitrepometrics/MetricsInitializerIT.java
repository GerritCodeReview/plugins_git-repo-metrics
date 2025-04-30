// Copyright (C) 2025 The Android Open Source Project
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

import static org.junit.Assert.fail;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.UseLocalDisk;
import com.google.gerrit.acceptance.WaitUtil;
import com.google.gerrit.acceptance.config.GlobalPluginConfig;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.FSMetricsCollector;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitRefsMetricsCollector;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitStatsMetricsCollector;
import java.time.Duration;
import org.junit.After;
import org.junit.Test;

@TestPlugin(
    name = "git-repo-metrics",
    sysModule = "com.googlesource.gerrit.plugins.gitrepometrics.Module")
public class MetricsInitializerIT extends LightweightPluginDaemonTest {

  private final int MAX_WAIT_TIME_FOR_METRICS_SECS = 5;

  @Inject MetricRegistry metricRegistry;
  private FSMetricsCollector fsMetricsCollector;
  private GitStatsMetricsCollector gitStatsMetricsCollector;
  private GitRefsMetricsCollector gitRefsMetricsCollector;
  private Slf4jReporter metricReporter;

  @Override
  public void setUpTestPlugin() throws Exception {
    super.setUpTestPlugin();

    fsMetricsCollector = plugin.getSysInjector().getInstance(FSMetricsCollector.class);
    gitStatsMetricsCollector = plugin.getSysInjector().getInstance(GitStatsMetricsCollector.class);
    gitRefsMetricsCollector = plugin.getSysInjector().getInstance(GitRefsMetricsCollector.class);

    metricReporter = Slf4jReporter.forRegistry(metricRegistry).build();
  }

  @After
  public void tearDown() {
    metricReporter.close();
  }

  @Test
  @UseLocalDisk
  @GlobalPluginConfig(
      pluginName = "git-repo-metrics",
      name = "git-repo-metrics.collectAllRepositories",
      value = "true")
  public void shouldCollectAllRepositoriesMetrics() {
    long ALL_PROJECTS_ALL_USERS_TEST_REPO_INITIAL_NUM_REPOS = 3L;
    int expectedMetricsCount =
        fsMetricsCollector.availableMetrics().size()
            + gitStatsMetricsCollector.availableMetrics().size()
            + gitRefsMetricsCollector.availableMetrics().size();

    try {
      WaitUtil.waitUntil(
          () ->
              getPluginMetricsCount()
                  == ALL_PROJECTS_ALL_USERS_TEST_REPO_INITIAL_NUM_REPOS * expectedMetricsCount,
          Duration.ofSeconds(MAX_WAIT_TIME_FOR_METRICS_SECS));
    } catch (InterruptedException e) {
      fail(
          String.format(
              "Only %d metrics have been registered, expected %d",
              getPluginMetricsCount(),
              ALL_PROJECTS_ALL_USERS_TEST_REPO_INITIAL_NUM_REPOS * expectedMetricsCount));
    }
  }

  private long getPluginMetricsCount() {
    metricReporter.report();
    return metricRegistry.getMetrics().keySet().stream()
        .filter(metricName -> metricName.contains("plugins/git-repo-metrics"))
        .count();
  }
}
