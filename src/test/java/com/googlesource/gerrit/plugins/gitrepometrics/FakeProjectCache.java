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

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSortedSet;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.Project.NameKey;
import com.google.gerrit.server.project.NullProjectCache;

public class FakeProjectCache extends NullProjectCache {
  private Set<Project.NameKey> projects;

  @Override
  public ImmutableSortedSet<NameKey> all() {
    return ImmutableSortedSet.copyOf(projects);
  }

  public void setProjectCount(int projectCount) {
    projects =
        IntStream.range(0, projectCount)
            .mapToObj(i -> NameKey.parse(String.valueOf(i)))
            .collect(Collectors.toSet());
  }

  public FakeProjectCache(int count) {
    setProjectCount(count);
  }
}
