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
package com.googlesource.gerrit.plugins.gitrepometrics;

import static org.junit.Assert.assertTrue;

import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.codahale.metrics.MetricRegistry;
import com.google.gerrit.extensions.common.ProjectInfo;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.server.restapi.project.ListProjects;
import com.google.gerrit.server.restapi.project.ListProjectsImpl;
import com.google.inject.Provider;
import com.google.inject.util.Providers;

@RunWith(MockitoJUnitRunner.class)
public class RepoCountMetricTest {
  private FakeMetricMaker fakeMetricMaker;
  private MetricRegistry metricRegistry;
  private Config gerritConfig;

  @Before
  public void setupRepo() {
    metricRegistry = new MetricRegistry();
    fakeMetricMaker = new FakeMetricMaker(metricRegistry);
    gerritConfig = new Config();
  }

  @Test
  public void metricIsCorrectlyRegistered() {
    RepoCountMetricRegister repoCountMetricRegister =
        new RepoCountMetricRegister(getWorkingProjectList(0), metricRegistry, fakeMetricMaker);

    repoCountMetricRegister.start();

    assertTrue(metricExists(RepoCountMetricRegister.REPO_COUNT_METRIC_NAME));
  }

  @Test(expected = Test.None.class)
  public void metricDoesNotBlowIfDuplicate() {
    RepoCountMetricRegister repoCountMetricRegister =
        new RepoCountMetricRegister(getWorkingProjectList(0), metricRegistry, fakeMetricMaker);

    repoCountMetricRegister.start();
    repoCountMetricRegister.start();

    assertTrue(metricExists(RepoCountMetricRegister.REPO_COUNT_METRIC_NAME));
  }

  private boolean metricExists(String metricName) {
    return metricRegistry
        .getMetrics()
        .containsKey(String.format("%s/%s/%s", "plugins", "git-repo-metrics", metricName));
  }

  private Provider<ListProjects> getWorkingProjectList(long numberProjects) {
    return Providers.of(
        new ListProjectsImpl(
            null, null, null, null, null, null, null, null, null, gerritConfig, null) {

          @Override
          public SortedMap<String, ProjectInfo> apply() throws BadRequestException {
            SortedMap<String, ProjectInfo> projects = new TreeMap<>();
            for (int i = 0; i < numberProjects; i++) {
              projects.put(String.valueOf(i), new ProjectInfo());
            }

            return projects;
          }
        });
  }
}
