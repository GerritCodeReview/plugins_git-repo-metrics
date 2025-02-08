# Plugin to collect Git repository metrics

This plugin allows a systematic collection of repository metrics for multiple Git SCM systems.
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

## How to install with Gerrit

Copy the git-repo-metrics.jar into the Gerrit's /plugins directory and wait for the plugin to be automatically
loaded.

## How to install with a different SCM

It's possible to make this plugin work with other Git repositories hosted by other Git based SCM tools,
however a read-only version of Gerrit will always be required.
So to make this plugin work with other Git SCM tools, a Gerrit installation needs to be set-up and the `basePath`
needs to be set to the git data directory of the tool of choice.
Also, a configuration option will need to be specified to indicate which Backend is being used.
Currently supported backend, other than Gerrit are:
- GitLab

## Configuration

More information about the plugin configuration can be found in the [config.md](src/resources/Documentation/config.md)
file.
