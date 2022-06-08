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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.gerrit.server.config.PluginConfigFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.lib.Config;
import org.junit.Ignore;

@Ignore
public class ConfigSetupUtils {
  static final String pluginName = "git-repo-metrics";
  private final Path basePath;
  private final Path gitBasePath;
  private final List<String> projects;
  private final String gracePeriod;

  public ConfigSetupUtils(List<String> projects) throws IOException {
    this(projects, "0");
  }

  public ConfigSetupUtils(List<String> projects, String gracePeriod) throws IOException {
    this.basePath = Files.createTempDirectory("git_repo_metrics_");
    this.gitBasePath = new File(basePath.toFile(), "git").toPath();
    this.projects = projects;
    this.gracePeriod = gracePeriod;
  }

  public GitRepoMetricsConfig getGitRepoMetricsConfig() {
    PluginConfigFactory pluginConfigFactory = mock(PluginConfigFactory.class);

    doReturn(getConfig()).when(pluginConfigFactory).getGlobalPluginConfig(any());

    return new GitRepoMetricsConfig(pluginConfigFactory, "git-repo-metrics");
  }

  public Config getConfig() {
    Config c = new Config();

    c.setStringList(pluginName, null, "project", projects);
    c.setString(pluginName, null, "gracePeriod", gracePeriod);
    c.setString("gerrit", null, "basePath", gitBasePath.toString());
    return c;
  }

  public Path getBasePath() {
    return basePath;
  }

  public Path getGitBasePath() {
    return gitBasePath;
  }
}
