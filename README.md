# pydantic-pycharm-plugin
Jetbrains Pycharm plugin for [pydantic](https://github.com/samuelcolvin/pydantic)

## original issue
 [Auto-completion when instantiating BaseModel objects #650](https://github.com/samuelcolvin/pydantic/issues/650)

## example:
![type check1](https://raw.githubusercontent.com/koxudaxi/pydantic-pycharm-plugin/master/docs/typecheck1.png)

## How to install:
There is a plugin in this git repo as [pydantic-pycharm-plugin.zip(latest)](https://github.com/koxudaxi/pydantic-pycharm-plugin/releases/latest/download/pydantic-pycharm-plugin.zip) 
You can install this plugin from disk
https://www.jetbrains.com/help/pycharm/plugins-settings.html
 
## Development
### Build plugin
You can build and run the plugin on your terminal or Intellij.
#### Shell on Linux or MacOS 
```bash
$ ./gradlew buildPlugin
```

#### Command Prompt on Windows
```
$ gradlew.bat buildPlugin
```

#### JetBrains IDE on Any platform

[official documents](https://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/using_dev_kit.html])

### Run IDE with build Plugin
```bash
$ ./gradlew runIde
```

## Features
### Implemented
#### pydantic.BaseModel
- `__init__` (partial)



## This project is an experimental phase.
