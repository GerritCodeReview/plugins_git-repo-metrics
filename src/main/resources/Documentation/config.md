@PLUGIN@ configuration
======================

The @PLUGIN@ allows a systematic collection of repository metrics.
Metrics are updated either upon a `ref-update` receive or on a time based refresh interval.
`ref-update` events are received only on primary nodes, so on replicas `gracePeriod` will need to be set.

Currently, the metrics exposed are the following:

```bash
plugins_git_repo_metrics_numberofbitmaps_<repo_name>
plugins_git_repo_metrics_numberoflooseobjects_<repo_name>
plugins_git_repo_metrics_numberoflooserefs_<repo_name>
plugins_git_repo_metrics_numberofpackedobjects_<repo_name>
plugins_git_repo_metrics_numberofpackedrefs_<repo_name>
plugins_git_repo_metrics_numberofpackfiles_<repo_name>
plugins_git_repo_metrics_sizeoflooseobjects_<repo_name>
plugins_git_repo_metrics_sizeofpackedobjects_<repo_name>
plugins_git_repo_metrics_numberofkeepfiles_<repo_name>
plugins_git_repo_metrics_numberoffiles_<repo_name>
plugins_git_repo_metrics_numberofdirectories_<repo_name>
plugins_git_repo_metrics_numberofemptydirectories_<repo_name>
plugins_git_repo_metrics_combinedrefssha1_<repo_name>
plugins_git_repo_metrics_numberofobjectssincebitmap_<repo_name>
plugins_git_repo_metrics_numberofpackfilessincebitmap_<repo_name>
```

> **NOTE**: The `<repo_name>` is a subject of sanitization in order to avoid collision between repository names.
> Rules are:
> - any character outside `[a-zA-Z0-9_-]+([a-zA-Z0-9_-]+)*` pattern is replaced with `_0x[HEX CODE]_` (code is capitalized)
>   string for instance `repo/name` is sanitized to `repo_0x2F_name`
> - if the repository name contains the replacement prefix `(_0x)` it is prefixed with another `_0x`
>   e.g. `repo_0x2F_name` becomes `repo_0x_0x2F_name`.

Settings
--------

The plugin allows to customize its behaviour through a specific
`git-repo-metrics.config` file in the `$GERRIT_SITE/etc` directory.

Following an example of config file:

```
[git-repo-metrics]
  project = test-project
  project = another-repo
  gracePeriod = 5m
  backend = GERRIT
```
_git-repo-metrics.project_: Project to collect metrics for. Multiple projects can be listed.

_git-repo-metrics.collectAllRepositories_: Collect metrics for all the repositories. By default, false.

_git-repo-metrics.forcedCollection_: Force the repositories' metric collection update every
_gracePeriod_ interval. By default, disabled.

> **NOTE**: When using `forcedCollection` the `gracePeriod` should be defined to a positive
> interval, otherwise the collection would happen just once at the plugin startup time.

_git-repo-metrics.gracePeriod_: Grace period between samples collection. Used to avoid aggressive
metrics collection. By default, 0.

_git-repo-metrics.poolSize_: Number of threads available to collect metrics. By default, 1.
_git-repo-metrics.gitBackend_: Name of the Git SCM tool managing the Git data, for which this tools will expose
metrics.

Currently supported values:
- GERRIT (default)
- GITLAB
