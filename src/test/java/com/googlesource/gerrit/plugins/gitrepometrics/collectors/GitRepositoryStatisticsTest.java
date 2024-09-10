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

import static com.google.common.truth.Truth.assertThat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.eclipse.jgit.api.GarbageCollectCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.internal.storage.file.GitRepositoryStatistics;
import org.eclipse.jgit.internal.storage.file.ObjectDirectory;
import org.eclipse.jgit.internal.storage.file.Pack;
import org.eclipse.jgit.internal.storage.file.PackFile;
import org.eclipse.jgit.internal.storage.file.PackIndex.MutableEntry;
import org.eclipse.jgit.internal.storage.pack.PackExt;
import org.eclipse.jgit.internal.storage.pack.PackWriter;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GitRepositoryStatisticsTest {
  @Rule public TemporaryFolder dir = new TemporaryFolder();

  private TestRepository<FileRepository> repository;

  @Before
  public void setUp() throws Exception {
    repository = createRepository("repo-stats");
  }

  @Test
  public void testShouldReportZeroObjectsForInitializedRepo() {
    // when
    long result = GitRepositoryStatistics.numberOfPackFilesAfterBitmap(repository.getRepository());

    // then
    assertThat(result).isEqualTo(0L);
  }

  @Test
  public void testShouldReportAllPackFilesWhenNoGcWasPerformed() throws Exception {
    // given
    packAndPrune();

    // when
    long result = GitRepositoryStatistics.numberOfPackFilesAfterBitmap(repository.getRepository());

    // then
    assertThat(result).isGreaterThan(0L);
    assertThat(result).isEqualTo(repository.getRepository().getObjectDatabase().getPacks().size());
  }

  @Test
  public void testShouldReportNoObjectsDirectlyAfterGc() throws Exception {
    // given
    addCommit(null);
    callGc();
    assertThat(repositoryBitmapFiles()).isEqualTo(1L);

    // when
    long result = GitRepositoryStatistics.numberOfPackFilesAfterBitmap(repository.getRepository());

    // then
    assertThat(result).isEqualTo(0L);
  }

  @Test
  public void testShouldReportNewObjectsAfterGcWhenRepositoryProgresses() throws Exception {
    // given
    RevCommit parent = addCommit(null);
    callGc();
    assertThat(repositoryBitmapFiles()).isEqualTo(1L);

    // progress the repository
    addCommit(parent);
    packAndPrune();

    // when
    long result = GitRepositoryStatistics.numberOfPackFilesAfterBitmap(repository.getRepository());

    // then
    assertThat(result).isEqualTo(1L);
  }

  @Test
  public void testShouldReportNewObjectsFromTheLatestBitmapWhenRepositoryProgresses()
      throws Exception {
    // given
    RevCommit parent = addCommit(null);
    callGc();
    assertThat(repositoryBitmapFiles()).isEqualTo(1L);

    // progress the repository and call GC again
    parent = addCommit(parent);
    callGc();
    assertThat(repositoryBitmapFiles()).isEqualTo(2L);

    // progress the repository again
    addCommit(parent);
    packAndPrune();

    // when
    long result = GitRepositoryStatistics.numberOfPackFilesAfterBitmap(repository.getRepository());

    // then
    assertThat(result).isEqualTo(1L);
  }

  private long repositoryBitmapFiles() throws IOException {
    return StreamSupport.stream(
            Files.newDirectoryStream(
                    repository.getRepository().getObjectDatabase().getPackDirectory().toPath(),
                    "pack-*.bitmap")
                .spliterator(),
            false)
        .count();
  }

  private RevCommit addCommit(RevCommit parent) throws Exception {
    PersonIdent ident = new PersonIdent("repo-metrics", "repo@metrics.com");
    TestRepository<FileRepository>.CommitBuilder builder = repository.commit().author(ident);
    if (parent != null) {
      builder.parent(parent);
    }
    RevCommit commit = builder.create();
    repository.update("master", commit);
    parent = commit;
    return parent;
  }

  private void callGc() throws GitAPIException {
    GarbageCollectCommand gc = repository.git().gc();
    gc.call();
  }

  /**
   * The TestRepository has a `packAndPrune` function but it fails in the last step after GC was
   * performed as it doesn't SKIP_MISSING files. In order to circumvent it was copied and improved
   * here.
   */
  private void packAndPrune() throws Exception {
    FileRepository db = repository.getRepository();
    ObjectDirectory odb = repository.getRepository().getObjectDatabase();
    NullProgressMonitor m = NullProgressMonitor.INSTANCE;

    final PackFile pack, idx;
    try (PackWriter pw = new PackWriter(db)) {
      Set<ObjectId> all = new HashSet<>();
      for (Ref r : db.getRefDatabase().getRefs()) all.add(r.getObjectId());
      pw.preparePack(m, all, PackWriter.NONE);

      pack = new PackFile(odb.getPackDirectory(), pw.computeName(), PackExt.PACK);
      try (OutputStream out = new BufferedOutputStream(new FileOutputStream(pack))) {
        pw.writePack(m, m, out);
      }
      pack.setReadOnly();

      idx = pack.create(PackExt.INDEX);
      try (OutputStream out = new BufferedOutputStream(new FileOutputStream(idx))) {
        pw.writeIndex(out);
      }
      idx.setReadOnly();
    }

    odb.openPack(pack);
    repository.updateServerInfo();
    for (Pack p : odb.getPacks()) {
      for (MutableEntry e : p)
        FileUtils.delete(odb.fileFor(e.toObjectId()), FileUtils.SKIP_MISSING);
    }
  }

  private TestRepository<FileRepository> createRepository(String prefix) throws Exception {
    File repo = dir.newFolder(String.format("%s-%d.git", prefix, System.currentTimeMillis()));
    try (Git git = Git.init().setDirectory(repo).call()) {
      return new TestRepository<>((FileRepository) git.getRepository());
    }
  }
}
