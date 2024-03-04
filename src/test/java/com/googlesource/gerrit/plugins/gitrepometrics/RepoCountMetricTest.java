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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.codahale.metrics.MetricRegistry;
import com.google.gerrit.server.project.NullProjectCache;

@RunWith(MockitoJUnitRunner.class)
public class RepoCountMetricTest {
  private FakeMetricMaker fakeMetricMaker;
  private MetricRegistry metricRegistry;
  private NullProjectCache nullProjectCache;

  @Before
  public void setupRepo() {
    metricRegistry = new MetricRegistry();
    fakeMetricMaker = new FakeMetricMaker(metricRegistry);
  }

  @Test
  public void metricIsCorrectlyRegistered() {
    RepoCountMetricRegister repoCountMetricRegister =
        new RepoCountMetricRegister(nullProjectCache, fakeMetricMaker);

    repoCountMetricRegister.start();
   
    assertTrue(metricExists(RepoCountMetricRegister.REPO_COUNT_METRIC_NAME));
  }

  private boolean metricExists(String metricName) {
    return metricRegistry
        .getMetrics()
        .containsKey(String.format("%s/%s/%s", "plugins", "git-repo-metrics", metricName));
  }
}
