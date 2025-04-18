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

import static java.util.stream.Collectors.toList;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.lib.Config;

@Singleton
public class GitRepoMetricsConfig {
  private final String pluginName;
  private final Config config;

  @Inject
  public GitRepoMetricsConfig(PluginConfigFactory configFactory, @PluginName String pluginName) {
    config = configFactory.getGlobalPluginConfig(pluginName);
    this.pluginName = pluginName;
  }

  public List<String> getRepositoryNames() {
    return Arrays.stream(config.getStringList(pluginName, null, "project")).collect(toList());
  }

  public Long getGracePeriodMs() {
    return config.getTimeUnit(pluginName, null, "gracePeriod", 0L, TimeUnit.MILLISECONDS);
  }

  public boolean isForcedCollection() {
    return config.getBoolean(pluginName, "forcedCollection", false);
  }

  public int getPoolSize() {
    return config.getInt(pluginName, null, "poolSize", 1);
  }

  public boolean collectAllRepositories() {
    return config.getBoolean(pluginName, null, "collectAllRepositories", false);
  }

  public GitBackend getGitBackend() {
    return config.getEnum(pluginName, null, "gitBackend", GitBackend.GERRIT);
  }
}
