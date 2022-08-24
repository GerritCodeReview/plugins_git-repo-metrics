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

import static com.google.common.truth.Truth.assertThat;

import com.codahale.metrics.MetricRegistry;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.UseLocalDisk;
import com.google.gerrit.acceptance.config.GlobalPluginConfig;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.FSMetricsCollector;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitStatsMetricsCollector;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

@TestPlugin(
    name = "git-repo-metrics",
    sysModule = "com.googlesource.gerrit.plugins.gitrepometrics.Module")
public class GitRepoMetricsCacheIT extends LightweightPluginDaemonTest {

  @Inject MetricRegistry metricRegistry;

  @Test
  @UseLocalDisk
  @GlobalPluginConfig(
      pluginName = "git-repo-metrics",
      name = "git-repo-metrics.project",
      values = {"test1", "test2"})
  public void shouldRegisterAllMetrics() {
    List<String> availableProjects = Arrays.asList("test1", "test2");
    List<String> repoMetricsCount =
        metricRegistry.getMetrics().keySet().stream()
            .filter(metricName -> metricName.contains("git-repo-metrics"))
            .collect(Collectors.toList());

    int expectedMetricsCount =
        new GitStatsMetricsCollector().availableMetrics().size()
            + new FSMetricsCollector().availableMetrics().size();

    assertThat(repoMetricsCount.size()).isEqualTo(availableProjects.size() * expectedMetricsCount);
  }
}
