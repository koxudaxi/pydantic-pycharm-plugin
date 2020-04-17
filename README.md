# pydantic-pycharm-plugin
[![Build Status](https://travis-ci.org/koxudaxi/pydantic-pycharm-plugin.svg?branch=master)](https://travis-ci.org/koxudaxi/pydantic-pycharm-plugin)
[![](https://img.shields.io/jetbrains/plugin/v/12861)](https://plugins.jetbrains.com/plugin/12861-pydantic)
[![codecov](https://codecov.io/gh/koxudaxi/pydantic-pycharm-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/koxudaxi/pydantic-pycharm-plugin)
![license](https://img.shields.io/github/license/koxudaxi/pydantic-pycharm-plugin.svg)

[A JetBrains PyCharm plugin](https://plugins.jetbrains.com/plugin/12861-pydantic) for [`pydantic`](https://github.com/samuelcolvin/pydantic).

(See [Auto-completion when instantiating BaseModel objects #650](https://github.com/samuelcolvin/pydantic/issues/650) for motivation.)


## Example:
![type check1](https://raw.githubusercontent.com/koxudaxi/pydantic-pycharm-plugin/master/docs/typecheck1.png)

##  Features
### Implemented
#### pydantic.BaseModel
* Model-specific `__init__`-signature inspection and autocompletion for subclasses of `pydantic.BaseModel`
* Model-specific `__init__`-arguments type-checking for subclasses of `pydantic.BaseModel` 
* Refactor support for renaming fields for subclasses of `BaseModel`
  * (If the field name is refactored from the model definition or `__init__` call keyword arguments, PyCharm will present a dialog offering the choice to automatically rename the keyword where it occurs in a model initialization call.
* Search related-fields by class attributes and keyword arguments of `__init__` with `Ctrl+B` and `Cmd+B`
* Provide an inspection for type-checking which is compatible with pydantic. the inspection supports `parsable-type`. the detail is at [Inspection for type-checking section](#inspection-for-type-checking)
#### pydantic.dataclasses.dataclass
* Support same features as `pydantic.BaseModel`
  * (After PyCharm 2020.1 and this plugin version 0.1.0, PyCharm treats `pydantic.dataclasses.dataclass` as third-party dataclass.)

### Inspection for type-checking
This plugin provides an inspection for type-checking, which is compatible with pydantic.
You can use the inspection on PyCharm's Settings (Preference -> Editor -> Inspections -> `Type checker compatible with Pydantic`) 
This inspection inherits from PyCharm's built-in type checker (aka `Type checker`).
Please disable `Type checker` when you enable `Type checker compatible with Pydantic.`
Don't use this type checker with a builtin type checker same time.

![inspection 1](https://raw.githubusercontent.com/koxudaxi/pydantic-pycharm-plugin/master/docs/inspection1.png)

### Parsable Type
Pydantic has lots of support for coercing types. However, PyCharm  gives a message saying only `Expected type "x," got "y" instead:`
When you set parsable-type on a type, then the message will be changed to `Field is of type "x", "y" may not be parsable to "x"`

![parsable type1](https://raw.githubusercontent.com/koxudaxi/pydantic-pycharm-plugin/master/docs/parsable-type1.png)

#### Set parsable-type in pyproject.toml
You should create `pyproject.toml` in your project root.
And, you define parsable-type like a example.

exapmle:

```toml
[tool.pydantic-pycharm-plugin.parsable-types]

# str field may parse int and float
str = ["int", "float"]

# datetime.datetime field may parse int
datetime.datetime = [ "int" ]
```

#### Related issues
- [reflect pydantic's type leniency #36](https://github.com/koxudaxi/pydantic-pycharm-plugin/issues/36)
- [Checking type with Enum, HttpUrl, conlist #99](https://github.com/koxudaxi/pydantic-pycharm-plugin/issues/99)

## How to install:
### MarketPlace 
The plugin is in Jetbrains repository ([Pydantic Plugin Page](https://plugins.jetbrains.com/plugin/12861-pydantic))

You can install the stable version on PyCharm's `Marketplace` (Preference -> Plugins -> Marketplace) [Offical Document](https://www.jetbrains.com/help/idea/managing-plugins.html)

### Complied binary
The releases section of this repository contains a compiled version of the plugin: [pydantic-pycharm-plugin.zip(latest)](https://github.com/koxudaxi/pydantic-pycharm-plugin/releases/latest/download/pydantic-pycharm-plugin.zip)

After downloading this file, you can install the plugin from disk by following [the JetBrains instructions here](https://www.jetbrains.com/help/pycharm/plugins-settings.html).

### Source
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

### Running the IDE with the built plugin
```bash
$ ./gradlew runIde
```


## Contribute
We are waiting for your contributions to `lpydantic-pycharm-plugin`.


## Links
### JetBrains Plugin Page
[Pydantic Plugin Page](https://plugins.jetbrains.com/plugin/12861-pydantic)

## This project is currently in an experimental phase
