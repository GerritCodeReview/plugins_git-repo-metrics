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

import static org.junit.Assert.fail;

import com.codahale.metrics.MetricRegistry;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.UseLocalDisk;
import com.google.gerrit.acceptance.WaitUtil;
import com.google.gerrit.acceptance.config.GlobalPluginConfig;
import com.google.gerrit.entities.Project;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.FSMetricsCollector;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitRefsMetricsCollector;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitStatsMetricsCollector;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

@TestPlugin(
    name = "git-repo-metrics",
    sysModule = "com.googlesource.gerrit.plugins.gitrepometrics.Module")
public class GitRepoMetricsCacheIT extends LightweightPluginDaemonTest {

  private final int MAX_WAIT_TIME_FOR_METRICS_SECS = 5;

  @Inject MetricRegistry metricRegistry;
  private FSMetricsCollector fsMetricsCollector;
  private GitStatsMetricsCollector gitStatsMetricsCollector;
  private GitRefsMetricsCollector gitRefsMetricsCollector;
  private GitRepoMetricsCache gitRepoMetricsCache;

  private final Project.NameKey testProject1 = Project.nameKey("testProject1");
  private final Project.NameKey testProject2 = Project.nameKey("testProject2");

  @Override
  public void setUpTestPlugin() throws Exception {
    super.setUpTestPlugin();

    repoManager.createRepository(testProject1).close();
    repoManager.createRepository(testProject2).close();
    gitRepoMetricsCache = plugin.getSysInjector().getInstance(GitRepoMetricsCache.class);
    fsMetricsCollector = plugin.getSysInjector().getInstance(FSMetricsCollector.class);
    gitStatsMetricsCollector = plugin.getSysInjector().getInstance(GitStatsMetricsCollector.class);
    gitRefsMetricsCollector = plugin.getSysInjector().getInstance(GitRefsMetricsCollector.class);
  }

  @Test
  @UseLocalDisk
  @GlobalPluginConfig(
      pluginName = "git-repo-metrics",
      name = "git-repo-metrics.project",
      values = {"testProject1", "testProject2"})
  public void shouldRegisterAllMetrics() throws IOException {
    ConfigSetupUtils configSetupUtils =
        new ConfigSetupUtils(Arrays.asList("testProject1", "testProject2"));
    List<Project.NameKey> availableProjects = Arrays.asList(testProject1, testProject2);
    new UpdateGitMetricsTask(
            gitRepoMetricsCache,
            repoManager,
            configSetupUtils.getGitRepoMetricsConfig(),
            testProject1.get())
        .run();
    new UpdateGitMetricsTask(
            gitRepoMetricsCache,
            repoManager,
            configSetupUtils.getGitRepoMetricsConfig(),
            testProject2.get())
        .run();

    int expectedMetricsCount =
        fsMetricsCollector.availableMetrics().size()
            + gitStatsMetricsCollector.availableMetrics().size()
            + gitRefsMetricsCollector.availableMetrics().size();

    try {
      WaitUtil.waitUntil(
          () -> getPluginMetricsCount() == (long) availableProjects.size() * expectedMetricsCount,
          Duration.ofSeconds(MAX_WAIT_TIME_FOR_METRICS_SECS));
    } catch (InterruptedException e) {
      fail(
          String.format(
              "Only %d metrics have been registered, expected %d",
              getPluginMetricsCount(), availableProjects.size() * expectedMetricsCount));
    }
  }

  private long getPluginMetricsCount() {
    return metricRegistry.getMetrics().keySet().stream()
        .filter(metricName -> metricName.contains("plugins/git-repo-metrics"))
        .count();
  }
}
