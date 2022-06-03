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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.util.concurrent.ExecutorService;

@Singleton
public class UpdateGitMetricsExecutorProvider implements Provider<ExecutorService> {
  private ExecutorService executor;

  @Inject
  UpdateGitMetricsExecutorProvider(WorkQueue workQueue, @PluginName String pluginName) {
    // TODO Make pool size configurable
    executor = workQueue.createQueue(1, "[" + pluginName + " plugin]");
  }

  @Override
  public ExecutorService get() {
    return executor;
  }
}
