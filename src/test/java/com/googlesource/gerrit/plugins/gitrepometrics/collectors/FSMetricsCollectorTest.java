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

package com.googlesource.gerrit.plugins.gitrepometrics.collectors;

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.entities.Project;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FSMetricsCollectorTest {

  @Rule public TemporaryFolder dir = new TemporaryFolder();

  private Repository repository;
  private Project.NameKey projectNameKey;
  private Project project;

  @Before
  public void setUp() throws Exception {
    projectNameKey = Project.NameKey.parse("testRepo");
    project = Project.builder(projectNameKey).build();
    repository = createRepository("someRepo.git");
  }

  @Test
  public void testCorrectMetricsCollection() throws IOException, InterruptedException {
    File objectDirectory = ((FileRepository) repository).getObjectsDirectory();
    Files.createFile(new File(objectDirectory, "pack/keep1.keep").toPath());

    HashMap<GitRepoMetric, Long> metrics = new HashMap<>();

    CountDownLatch latch = new CountDownLatch(1);
    new FSMetricsCollector(Executors.newFixedThreadPool(2))
        .collect(
            (FileRepository) repository,
            project,
            m -> {
              metrics.putAll(m);
              latch.countDown();
            });
    latch.await();

    // This is the FS structure, from the "objects" directory, metrics are collected from:
    //  .
    //  ├── info
    //  └── pack
    //      └── keep1.keep
    assertThat(metrics.get(FSMetricsCollector.numberOfKeepFiles)).isEqualTo(1); // keep1.keep
    assertThat(metrics.get(FSMetricsCollector.numberOfFiles)).isEqualTo(1); // keep1.keep
    assertThat(metrics.get(FSMetricsCollector.numberOfDirectories))
        .isEqualTo(3); // info, pack and .
    assertThat(metrics.get(FSMetricsCollector.numberOfEmptyDirectories)).isEqualTo(1); // info
  }

  private FileRepository createRepository(String repoName) throws Exception {
    File repo = dir.newFolder(repoName);
    try (Git git = Git.init().setDirectory(repo).call()) {
      return (FileRepository) git.getRepository();
    }
  }
}
