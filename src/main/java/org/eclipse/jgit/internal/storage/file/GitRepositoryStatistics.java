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

package org.eclipse.jgit.internal.storage.file;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;

public class GitRepositoryStatistics {
  public static long numberOfPackFilesAfterBitmap(FileRepository repository) {
    Collection<Pack> packs = repository.getObjectDatabase().getPacks();
    return getNewestBitmapTime(packs)
        .map(
            newestBitmap ->
                packs.stream()
                    .filter(Predicate.not(GitRepositoryStatistics::hasBitmap))
                    .map(pack -> pack.getFileSnapshot().lastModifiedInstant().toEpochMilli())
                    .filter(packTime -> packTime > newestBitmap)
                    .count())
        .orElseGet(() -> Long.valueOf(packs.size()));
  }

  private static Optional<Long> getNewestBitmapTime(Collection<Pack> packs) {
    return packs.stream()
        .filter(GitRepositoryStatistics::hasBitmap)
        .map(p -> p.getFileSnapshot().lastModifiedInstant().toEpochMilli())
        .max(Comparator.naturalOrder());
  }

  private static boolean hasBitmap(Pack pack) {
    try {
      return pack.getBitmapIndex() != null;
    } catch (IOException e) {
      return false;
    }
  }

  private GitRepositoryStatistics() {}
}
