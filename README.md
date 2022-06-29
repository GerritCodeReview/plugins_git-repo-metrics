# Plugin to collect Git repository metrics

This plugin allows a systematic collection of repository metrics.
Metrics are updated upon a `ref-update` receive.

## How to build

Clone or link this plugin to the plugins directory of Gerrit's source tree, and then run bazel build
on the plugin's directory.

Example:

```
git clone --recursive https://gerrit.googlesource.com/gerrit
git clone https://gerrit.googlesource.com/plugins/git-repo-metrics
pushd gerrit/plugins && ln -s ../../git-repo-metrics . && popd
cd gerrit && bazel build plugins/git-repo-metrics
```

The output plugin jar is created in:

```
bazel-genfiles/plugins/git-repo-metrics/git-repo-metrics.jar
```

## How to install

Copy the git-repo-metrics.jar into the Gerrit's /plugins directory and wait for the plugin to be automatically
loaded.

## Configuration

More information about the plugin configuration can be found in the [config.md](resources/Documentation/config.md)
file.
