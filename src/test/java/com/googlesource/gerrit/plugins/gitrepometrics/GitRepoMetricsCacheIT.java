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
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.MetricsCollector;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = "git-repo-metrics",
    sysModule = "com.googlesource.gerrit.plugins.gitrepometrics.Module")
public class GitRepoMetricsCacheIT extends LightweightPluginDaemonTest {

  @Inject MetricRegistry metricRegistry;
  @Inject PluginConfigFactory config;
  private Repository testRepo1;
  private Repository testRepo2;
  GitRepoMetricsCache gitRepoMetricsCache;
  private DynamicSet<MetricsCollector> ds;

  private final Project.NameKey testProject1 = Project.nameKey("testProject1");
  private final Project.NameKey testProject2 = Project.nameKey("testProject2");

  @Override
  @Before
  public void setUpTestPlugin() throws Exception {
    super.setUpTestPlugin();

    testRepo1 = repoManager.createRepository(testProject1);
    testRepo2 = repoManager.createRepository(testProject2);

    ds = new DynamicSet<MetricsCollector>();
    ds.add("git-repo-metrics", new FakeMetricsCollector("collector1"));
    ds.add("git-repo-metrics", new FakeMetricsCollector("collector2"));

    gitRepoMetricsCache =
        new GitRepoMetricsCache(
            ds,
            new FakeMetricMaker(metricRegistry),
            metricRegistry,
            new GitRepoMetricsConfig(config, ConfigSetupUtils.pluginName));
  }

  @Test
  @UseLocalDisk
  @GlobalPluginConfig(
      pluginName = "git-repo-metrics",
      name = "git-repo-metrics.project",
      values = {"testProject1", "testProject2"})
  public void shouldRegisterAllMetrics() {
    new UpdateGitMetricsTask(gitRepoMetricsCache, repoManager, "testProject1").run();
    new UpdateGitMetricsTask(gitRepoMetricsCache, repoManager, "testProject2").run();

    List<String> repoMetricsCount =
        metricRegistry.getMetrics().keySet().stream()
            .filter(metricName -> metricName.contains("git-repo-metrics"))
            .collect(Collectors.toList());
    // We expect 8 metrics since we have 2 projects (testProject1 and testProject1) and we have 2
    // collectors
    // collecting 2 metrics (see FakeStatsCollector)
    assertThat(repoMetricsCount.size())
        .isEqualTo(2 /* metrics */ * 2 /* repositories */ * 2 /* collectors */);
  }
}
