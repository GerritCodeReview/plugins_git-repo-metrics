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
import java.util.List;
import org.eclipse.jgit.lib.Config;

public class Utils {

  public static GitRepoMetricsConfig getRpoConfig(List<String> projects) {
    return getRpoConfig(projects, "0m");
  }

  public static GitRepoMetricsConfig getRpoConfig(List<String> projects, String gracePeriod) {
    PluginConfigFactory pluginConfigFactory = mock(PluginConfigFactory.class);

    Config c = new Config();

    c.setStringList("git-repo-metrics", null, "project", projects);
    c.setString("git-repo-metrics", null, "gracePeriod", gracePeriod);

    doReturn(c).when(pluginConfigFactory).getGlobalPluginConfig(any());

    return new GitRepoMetricsConfig(pluginConfigFactory, "git-repo-metrics");
  }
}
