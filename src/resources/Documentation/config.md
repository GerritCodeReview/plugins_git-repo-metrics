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

Settings
--------

The plugin allows to customize its behaviour through a specific
`git-repo-metrics.config` file in the `$GERRIT_SITE/etc` directory.

The metrics are not collected for all the projects, otherwise there might be an explosion of metrics
exported, but only the one listed in the configuration file, i.e.:

```
[git-repo-metrics]
  project = test-project
  project = another-repo
  gracePeriod = 5m
  backend = GERRIT
```
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
