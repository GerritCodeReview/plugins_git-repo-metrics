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
import static java.nio.file.Files.delete;

import com.codahale.metrics.MetricRegistry;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.SitePath;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.StatsCollector;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

public class UpdateGitMetricsTaskTest {
  private final String projectName = "testProject";
  private final Project.NameKey projectNameKey = Project.nameKey(projectName);
  private Repository testRepository;
  private Project testProject;
  private GitRepoMetricsCache gitRepoMetricsCache;
  private FakeStatsCollector fakeStatsCollector;
  private DynamicSet<StatsCollector> ds;

  @Inject private UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory;

  @Before
  public void setupRepository() throws Exception {
    ConfigSetupUtils configSetupUtils = new ConfigSetupUtils(Collections.singletonList("repo1"));

    fakeStatsCollector = new FakeStatsCollector();
    ds = new DynamicSet<>();
    ds.add("git-repo-metrics", fakeStatsCollector);

    gitRepoMetricsCache =
        new GitRepoMetricsCache(
            ds,
            new DisabledMetricMaker(),
            new MetricRegistry(),
            configSetupUtils.getGitRepoMetricsConfig());

    AbstractModule m =
        new AbstractModule() {
          @Override
          protected void configure() {
            install(new UpdateGitMetricsTaskModule());
            bind(GitRepoMetricsCache.class).toInstance(gitRepoMetricsCache);
            bind(Config.class)
                .annotatedWith(GerritServerConfig.class)
                .toInstance(configSetupUtils.getConfig());
          }

          @Provides
          @SitePath
          Path getSitePath() {
            return configSetupUtils.getBasePath();
          }
        };
    Injector injector = Guice.createInjector(m);
    injector.injectMembers(this);

    try {
      testRepository =
          new FileRepository(new File(configSetupUtils.getGitBasePath().toFile(), projectName));
      testRepository.create(true);
    } catch (Exception e) {
      delete(configSetupUtils.getGitBasePath());
      throw e;
    }
    testProject = Project.builder(projectNameKey).build();
  }

  @Test
  public void shouldUpdateMetrics() {
    UpdateGitMetricsTask updateGitMetricsTask = updateGitMetricsTaskFactory.create(projectName);
    updateGitMetricsTask.run();
    assertThat(gitRepoMetricsCache.getMetrics().keySet()).isNotEmpty();
  }

  @Test
  public void shouldNotUpdateMetricsIfRepoDoesNotExist() {
    UpdateGitMetricsTask updateGitMetricsTask =
        updateGitMetricsTaskFactory.create("nonExistentProject");
    updateGitMetricsTask.run();
    assertThat(gitRepoMetricsCache.getMetrics().keySet()).isEmpty();
  }
}
