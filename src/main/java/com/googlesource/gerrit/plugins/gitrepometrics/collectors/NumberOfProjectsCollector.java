// Copyright (C) 2024 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.gitrepometrics.collectors;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.util.ManualRequestContext;
import com.google.gerrit.server.util.OneOffRequestContext;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.gitrepometrics.GitRepoMetricsConfig;
import java.util.concurrent.TimeUnit;

@Singleton
public class NumberOfProjectsCollector implements Provider<Long> {
  public static final String NUM_PROJECTS = "num-projects";
  public static final long MIN_NUM_PROJECTS_GRACE_PERIOD_MS = 1000L;

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final Supplier<Long> numberOfProjects;

  @Inject
  NumberOfProjectsCollector(GerritApi api, OneOffRequestContext ctx, GitRepoMetricsConfig config) {
    numberOfProjects =
        Suppliers.memoizeWithExpiration(
            () -> queryNumberOfProjects(ctx, api),
            Math.max(MIN_NUM_PROJECTS_GRACE_PERIOD_MS, config.getGracePeriodMs()),
            TimeUnit.MILLISECONDS);
  }

  private static long queryNumberOfProjects(OneOffRequestContext ctx, GerritApi api) {
    try (ManualRequestContext c = ctx.open()) {
      return api.projects().query().get().size();
    } catch (RestApiException e) {
      logger.atWarning().withCause(e).log("Unable to query Gerrit projects list");
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Long get() {
    return numberOfProjects.get();
  }
}
