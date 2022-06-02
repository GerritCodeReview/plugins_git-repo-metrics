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

import com.google.gerrit.entities.Project;
import com.google.gerrit.metrics.DisabledMetricMaker;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

public class UpdateGitMetricsTaskTest {

  private final String projectName = "testProject";
  private final Project.NameKey projectNameKey = Project.nameKey(projectName);
  private Repository testRepository;
  private Project testProject;
  private GitRepoMetricsCache gitRepoMetricsCache;

  @Inject private UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory;

  @Before
  public void setupRepository() throws Exception {

    gitRepoMetricsCache =
        new GitRepoMetricsCache(
            new DisabledMetricMaker(), Utils.getRpoConfig(Collections.singletonList("repo1")));

    AbstractModule m =
        new AbstractModule() {
          @Override
          protected void configure() {
            install(new UpdateGitMetricsTaskModule());
            bind(GitRepoMetricsCache.class).toInstance(gitRepoMetricsCache);
          }
        };
    Injector injector = Guice.createInjector(m);
    injector.injectMembers(this);

    Path p = Files.createTempDirectory("git_repo_metrics_");
    try {
      testRepository = new FileRepository(p.toFile());
      testRepository.create(true);
    } catch (Exception e) {
      delete(p);
      throw e;
    }
    testProject = Project.builder(projectNameKey).build();
  }

  @Test
  public void shouldUpdateMetrics() {
    UpdateGitMetricsTask updateGitMetricsTask =
        updateGitMetricsTaskFactory.create(testRepository, testProject);
    updateGitMetricsTask.run();
    assertThat(gitRepoMetricsCache.getMetrics().keySet()).isNotEmpty();
  }
}
