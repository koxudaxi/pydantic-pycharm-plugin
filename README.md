# pydantic-pycharm-plugin
A JetBrains PyCharm plugin for [`pydantic`](https://github.com/samuelcolvin/pydantic).

(See [Auto-completion when instantiating BaseModel objects #650](https://github.com/samuelcolvin/pydantic/issues/650) for motivation.)

## Example:
![type check1](https://raw.githubusercontent.com/koxudaxi/pydantic-pycharm-plugin/master/docs/typecheck1.png)

## Features
### Implemented
#### pydantic.BaseModel
* Model-specific `__init__`-signature inspection and autocompletion for subclasses of `pydantic.BaseModel`
* Model-specific `__init__`-arguments type-checking for subclasses of `pydantic.BaseModel` 
* Refactor support for renaming fields for subclasses of `BaseModel`
  * (If the field name is refactored from the model definition, PyCharm will present a dialog offering the choice to automatically rename the keyword where it occurs in a model initialization call.


## How to install:
The releases section of this repository contains a compiled version of the plugin: [pydantic-pycharm-plugin.zip(latest)](https://github.com/koxudaxi/pydantic-pycharm-plugin/releases/latest/download/pydantic-pycharm-plugin.zip)

After downloading this file, you can install the plugin from disk by following [the JetBrains instructions here](https://www.jetbrains.com/help/pycharm/plugins-settings.html).

Alternatively, you can clone this repository and follow the instructions under the "Building the plugin" heading below to build from source. The build process will create the file `build/distributions/pydantic-pycharm-plugin.zip`. This file can be installed as a PyCharm plugin from disk following the same instructions.
 
## Development
### Building the plugin
You can build and run the plugin either via the command line or through IntelliJ IDEA:

#### Shell on Linux or MacOS 
```bash
$ ./gradlew buildPlugin
```

#### Command Prompt on Windows
```
$ gradlew.bat buildPlugin
```

#### JetBrains IDE on any platform

[Official documentation](https://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/using_dev_kit.html])

### Running the IDE with the built Plugin
```bash
$ ./gradlew runIde
```

## This project is currently in an experimental phase
