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
import static com.googlesource.gerrit.plugins.gitrepometrics.GitRepoMetricsCacheModule.metrics;
import static java.nio.file.Files.delete;

import com.google.gerrit.entities.Project;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Test;

public class UpdateGitMetricsTaskTest {

  private final String projectName = "testProject";
  private final Project.NameKey projectNameKey = Project.nameKey(projectName);
  private Repository testRepository;
  private Project testProject;

  @Before
  public void setupRepository() throws Exception {
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
        new UpdateGitMetricsTask(testRepository, testProject);
    updateGitMetricsTask.run();
    assertThat(metrics.keySet()).isNotEmpty();
  }
}
