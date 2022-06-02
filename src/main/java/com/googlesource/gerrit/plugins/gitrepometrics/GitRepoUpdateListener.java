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
import com.google.gerrit.extensions.events.GitReferenceUpdatedListener;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;

public class GitRepoUpdateListener implements GitReferenceUpdatedListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final GitRepositoryManager repoManager;
  private final ExecutorService executor;
  private final UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory;

  @Inject
  GitRepoUpdateListener(
      GitRepositoryManager repoManager,
      @UpdateGitMetricsExecutor ExecutorService executor,
      UpdateGitMetricsTask.Factory updateGitMetricsTaskFactory) {
    this.repoManager = repoManager;
    this.executor = executor;
    this.updateGitMetricsTaskFactory = updateGitMetricsTaskFactory;
  }

  @Override
  public void onGitReferenceUpdated(Event event) {
    String projectName = event.getProjectName();
    Project.NameKey projectNameKey = Project.nameKey(projectName);
    logger.atFine().log("Got an update for project %s", projectName);
    try (Repository repository = repoManager.openRepository(projectNameKey)) {
      UpdateGitMetricsTask updateGitMetricsTask =
          updateGitMetricsTaskFactory.create(repository, Project.builder(projectNameKey).build());
      executor.execute(updateGitMetricsTask);
    } catch (RepositoryNotFoundException e) {
      logger.atSevere().withCause(e).log("Cannot find repository for %s", projectName);
    } catch (IOException e) {
      logger.atSevere().withCause(e).log(
          "Something went wrong when reading from the repository for %s", projectName);
    }
  }
}
