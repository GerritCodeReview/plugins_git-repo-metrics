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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.codahale.metrics.MetricRegistry;
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

  @Override
  public void setUpTestPlugin() throws Exception {
    super.setUpTestPlugin();

    fsMetricsCollector = plugin.getSysInjector().getInstance(FSMetricsCollector.class);
    gitStatsMetricsCollector = plugin.getSysInjector().getInstance(GitStatsMetricsCollector.class);
    gitRefsMetricsCollector = plugin.getSysInjector().getInstance(GitRefsMetricsCollector.class);
  }

  @Test
  @UseLocalDisk
  @GlobalPluginConfig(
      pluginName = "git-repo-metrics",
      name = "git-repo-metrics.collectAllRepositories",
      value = "true")
  public void shouldCollectAllRepositoriesMetrics() {
    int expectedMetricsCount =
        fsMetricsCollector.availableMetrics().size()
            + gitStatsMetricsCollector.availableMetrics().size()
            + gitRefsMetricsCollector.availableMetrics().size();

    try {
      WaitUtil.waitUntil(
          () -> getPluginMetricsCount() == 2L * expectedMetricsCount,
          Duration.ofSeconds(MAX_WAIT_TIME_FOR_METRICS_SECS));
    } catch (InterruptedException e) {
      fail(
          String.format(
              "Only %d metrics have been registered, expected %d",
              getPluginMetricsCount(), 2L * expectedMetricsCount));
    }
  }

  @Test
  @UseLocalDisk
  @GlobalPluginConfig(
      pluginName = "git-repo-metrics",
      name = "git-repo-metrics.collectAllRepositories",
      value = "false")
  public void shouldNotCollectRepositoriesMetrics() {
    assertThat(getPluginMetricsCount()).isEqualTo(0);
  }

  private long getPluginMetricsCount() {
    return metricRegistry.getMetrics().keySet().stream()
        .filter(metricName -> metricName.contains("plugins/git-repo-metrics"))
        .count();
  }
}
