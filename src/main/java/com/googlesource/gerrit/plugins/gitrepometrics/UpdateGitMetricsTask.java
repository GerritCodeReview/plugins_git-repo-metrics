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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.git.DelegateRepository;
import com.google.gerrit.server.git.DelegateRepositoryUnwrapper;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.googlesource.gerrit.plugins.gitrepometrics.collectors.GitStats;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;

public class UpdateGitMetricsTask implements Runnable {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public interface Factory {
    UpdateGitMetricsTask create(String projectName);
  }

  private final String projectName;
  private GitRepoMetricsCache gitRepoMetricsCache;
  private GitRepositoryManager repoManager;

  @Inject
  UpdateGitMetricsTask(
      GitRepoMetricsCache gitRepoMetricsCache,
      GitRepositoryManager repoManager,
      @Assisted String projectName) {
    this.projectName = projectName;
    this.gitRepoMetricsCache = gitRepoMetricsCache;
    this.repoManager = repoManager;
  }

  @Override
  public void run() {
    Project.NameKey projectNameKey = Project.nameKey(projectName);
    try (Repository repository = repoManager.openRepository(projectNameKey)) {
      logger.atInfo().log(
          "Running task to collect stats: repo %s, project %s",
          repository.getIdentifier(), projectName);
      // TODO Loop through all the collectors
      Project project = Project.builder(projectNameKey).build();

      Repository unwrappedRepo =
          repository instanceof DelegateRepository
              ? DelegateRepositoryUnwrapper.unwrap((DelegateRepository) repository)
              : repository;

      GitStats gitStats = new GitStats((FileRepository) unwrappedRepo, project);
      Map<String, Long> newMetrics = gitStats.get();
      logger.atInfo().log(
          "Here all the metrics for %s - %s", project.getName(), getStringFromMap(newMetrics));
      gitRepoMetricsCache.setMetrics(newMetrics, projectName);
    } catch (RepositoryNotFoundException e) {
      logger.atSevere().withCause(e).log("Cannot find repository for %s", projectName);
    } catch (IOException e) {
      logger.atSevere().withCause(e).log(
          "Something went wrong when reading from the repository for %s", projectName);
    }
  }

  String getStringFromMap(Map<String, Long> m) {
    return m.keySet().stream()
        .map(key -> key + "=" + m.get(key))
        .collect(Collectors.joining(", ", "{", "}"));
  }

  @Override
  public String toString() {
    return "UpdateGitMetricsTask " + projectName;
  }
}
