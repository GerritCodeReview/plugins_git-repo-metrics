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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Supplier;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Before;
import org.junit.Test;

public class RepoCountMetricTest {
  private FakeMetricMaker fakeMetricMaker;
  private MetricRegistry metricRegistry;
  private String repoCountMetricName;

  @Before
  public void setup() {
    metricRegistry = new MetricRegistry();
    fakeMetricMaker = new FakeMetricMaker(metricRegistry);
    repoCountMetricName =
        String.format(
            "%s/%s/%s",
            "plugins", "git-repo-metrics", RepoCountMetricRegister.REPO_COUNT_METRIC_NAME);
  }

  @Test
  public void metricIsCorrectlyRegistered() {
    RepoCountMetricRegister repoCountMetricRegister =
        new RepoCountMetricRegister(() -> 1L, fakeMetricMaker);

    repoCountMetricRegister.start();

    assertTrue(metricRegistry.getMetrics().containsKey(repoCountMetricName));

    metricRegistry.remove(repoCountMetricName);
  }

  @Test
  public void metricIsUpdated() {
    AtomicLong numProjects = new AtomicLong();
    RepoCountMetricRegister repoCountMetricRegister =
        new RepoCountMetricRegister(numProjects::get, fakeMetricMaker);

    repoCountMetricRegister.start();

    assertEquals(1, fakeMetricMaker.actionMap.size());

    @SuppressWarnings("rawtypes")
    Optional<Supplier> obj = fakeMetricMaker.getValueForMetric(repoCountMetricName);

    assertTrue(!obj.isEmpty());
    assertEquals(0, ((Long) obj.get().get()).longValue());

    numProjects.set(2);
    assertEquals(2, ((Long) obj.get().get()).longValue());
  }
}
