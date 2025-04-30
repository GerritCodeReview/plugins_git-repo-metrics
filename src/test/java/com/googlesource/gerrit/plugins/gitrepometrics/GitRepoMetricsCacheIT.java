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
import com.codahale.metrics.Slf4jReporter;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.acceptance.UseLocalDisk;
import com.google.gerrit.acceptance.WaitUtil;
import com.google.gerrit.acceptance.config.GlobalPluginConfig;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.RefNames;
import com.google.gerrit.extensions.api.projects.BranchInfo;
import com.google.gerrit.extensions.api.projects.BranchInput;
import com.google.gerrit.extensions.api.projects.ProjectApi;
import com.google.gerrit.extensions.api.projects.ProjectInput;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.FSMetricsCollector;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitRefsMetricsCollector;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitStatsMetricsCollector;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.After;
import org.junit.Test;

@TestPlugin(
    name = "git-repo-metrics",
    sysModule = "com.googlesource.gerrit.plugins.gitrepometrics.Module")
public class GitRepoMetricsCacheIT extends LightweightPluginDaemonTest {

  public static final String NUM_LOOSE_REFS = "numberoflooserefs";
  private static final long HEAD_MASTER_REFS_META_CONFIG_NUM_REFS = 3;
  private final int MAX_WAIT_TIME_FOR_METRICS_SECS = 5;

  @Inject MetricRegistry metricRegistry;
  private FSMetricsCollector fsMetricsCollector;
  private GitStatsMetricsCollector gitStatsMetricsCollector;
  private GitRefsMetricsCollector gitRefsMetricsCollector;
  private GitRepoMetricsCache gitRepoMetricsCache;
  private Slf4jReporter metricReporter;

  private final Project.NameKey testProject1 = Project.nameKey("testProject1");
  private final Project.NameKey testProject2 = Project.nameKey("testProject2");

  @Override
  public void setUpTestPlugin() throws Exception {
    super.setUpTestPlugin();

    createProjectWithEmptyCommit(testProject1.get());
    createProjectWithEmptyCommit(testProject2.get());
    gitRepoMetricsCache = plugin.getSysInjector().getInstance(GitRepoMetricsCache.class);
    fsMetricsCollector = plugin.getSysInjector().getInstance(FSMetricsCollector.class);
    gitStatsMetricsCollector = plugin.getSysInjector().getInstance(GitStatsMetricsCollector.class);
    gitRefsMetricsCollector = plugin.getSysInjector().getInstance(GitRefsMetricsCollector.class);
    metricReporter = Slf4jReporter.forRegistry(metricRegistry).build();
  }

  @After
  public void tearDown() {
    metricReporter.close();
  }

  @CanIgnoreReturnValue
  private ProjectApi createProjectWithEmptyCommit(String projectName) throws RestApiException {
    ProjectInput pi = new ProjectInput();
    pi.name = projectName;
    pi.createEmptyCommit = true;
    return gApi.projects().create(pi);
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
          () -> {
            metricReporter.report();
            return getPluginMetricsCount() == (long) availableProjects.size() * expectedMetricsCount;
          },
          Duration.ofSeconds(MAX_WAIT_TIME_FOR_METRICS_SECS));
    } catch (InterruptedException e) {
      fail(
          String.format(
              "Only %d metrics have been registered, expected %d",
              getPluginMetricsCount(), availableProjects.size() * expectedMetricsCount));
    }
  }

  @Test
  @UseLocalDisk
  @GlobalPluginConfig(
      pluginName = "git-repo-metrics",
      name = "git-repo-metrics.project",
      value = "testProject1")
  public void shouldUpdateRepositoryMetricsOnTwoRefUpdates() throws Exception {
    addTwoBranchesToProjectAndAssertLooseRefsMetrics();
  }

  @Test
  @UseLocalDisk
  @GlobalPluginConfig(
      pluginName = "git-repo-metrics",
      name = "git-repo-metrics.project",
      value = "testProject1")
  @GlobalPluginConfig(
      pluginName = "git-repo-metrics",
      name = "git-repo-metrics.gracePeriod",
      value = "1 s")
  public void shouldUpdateRepositoryMetricsOnTwoRefUpdatesWithGracePeriod() throws Exception {
    addTwoBranchesToProjectAndAssertLooseRefsMetrics();
  }

  private void addTwoBranchesToProjectAndAssertLooseRefsMetrics()
      throws InterruptedException, RestApiException {
    long initialLooseRefs =
        waitForMetricValue(
            testProject1.get(), NUM_LOOSE_REFS, (v) -> v == HEAD_MASTER_REFS_META_CONFIG_NUM_REFS);

    createBranch("branch1");
    waitForMetricValue(
        testProject1.get(), NUM_LOOSE_REFS, (value) -> value == initialLooseRefs + 1);

    createBranch("branch2");
    waitForMetricValue(
        testProject1.get(), NUM_LOOSE_REFS, (value) -> value == initialLooseRefs + 2);
  }

  @CanIgnoreReturnValue
  private long waitForMetricValue(
      String projectName, String metric, Function<Long, Boolean> metricCondition)
      throws InterruptedException {
    AtomicReference<Optional<Long>> metricValue = new AtomicReference<>();
    WaitUtil.waitUntil(
        () -> {
          metricValue.set(gitRepoMetric(projectName, metric));
          return metricValue.get().map(metricCondition).orElse(false);
        },
        Duration.ofSeconds(MAX_WAIT_TIME_FOR_METRICS_SECS));
    return metricValue.get().get();
  }

  private void createBranch(String branchName) throws RestApiException {
    BranchInput bi = new BranchInput();
    bi.ref = RefNames.REFS_HEADS + branchName;
    bi.revision = "HEAD";
    BranchInfo unused = gApi.projects().name(testProject1.get()).branch(bi.ref).create(bi).get();
  }

  private long getPluginMetricsCount() {
    return metricRegistry.getMetrics().keySet().stream()
        .filter(metricName -> metricName.contains("plugins/git-repo-metrics"))
        .count();
  }

  private Optional<Long> gitRepoMetric(String projectName, String metricName) {
    return Optional.ofNullable(
        gitRepoMetricsCache
            .getMetrics()
            .get(metricName.toLowerCase())
            .get(projectName.toLowerCase()));
  }
}
