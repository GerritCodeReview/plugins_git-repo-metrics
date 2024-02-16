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

import static com.google.common.io.RecursiveDeleteOption.ALLOW_INSECURE;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.io.MoreFiles;
import java.io.File;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.PersonIdent;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GitRefsMetricsCollectorTest {
  private static final String REPO_NAME = "test-repo";

  @Rule public TemporaryFolder dir = new TemporaryFolder();

  private TestRepository<FileRepository> repo;

  @Before
  public void setUp() throws Exception {
    File gitRoot = dir.newFolder(REPO_NAME);
    try (Git git = Git.init().setDirectory(gitRoot).call()) {
      repo = new TestRepository<>((FileRepository) git.getRepository());
      repo.commit().author(new PersonIdent("repo-metrics", "repo@metrics.com")).create();
    }
  }

  @Test
  public void shouldComputeCombinedRefsSha1() throws Exception {
    HashMap<GitRepoMetric, Long> result = new HashMap<>();

    CountDownLatch latch = new CountDownLatch(1);
    new GitRefsMetricsCollector(Executors.newFixedThreadPool(1))
        .collect(
            repo.getRepository(),
            REPO_NAME,
            m -> {
              result.putAll(m);
              latch.countDown();
            });
    latch.await();

    assertThat(result.get(GitRefsMetricsCollector.combinedRefsSha1)).isEqualTo(16777109);
  }

  @After
  public void tearDown() throws Exception {
    repo.close();
    MoreFiles.deleteRecursively(dir.getRoot().toPath(), ALLOW_INSECURE);
  }
}
