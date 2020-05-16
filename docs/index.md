# Pydantic PyCharm Plugin
[![Build Status](https://travis-ci.org/koxudaxi/pydantic-pycharm-plugin.svg?branch=master)](https://travis-ci.org/koxudaxi/pydantic-pycharm-plugin)
[![](https://img.shields.io/jetbrains/plugin/v/12861)](https://plugins.jetbrains.com/plugin/12861-pydantic)
![JetBrains IntelliJ plugins](https://img.shields.io/jetbrains/plugin/d/12861-pydantic)
![JetBrains IntelliJ Plugins](https://img.shields.io/jetbrains/plugin/r/rating/12861-pydantic)
[![codecov](https://codecov.io/gh/koxudaxi/pydantic-pycharm-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/koxudaxi/pydantic-pycharm-plugin)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

[A JetBrains PyCharm plugin](https://plugins.jetbrains.com/plugin/12861-pydantic) for [`pydantic`](https://github.com/samuelcolvin/pydantic).

## Interview
I got interviewed about this plugin for [JetBrains' PyCharm Blog](https://blog.jetbrains.com/pycharm/2020/04/interview-koudai-aono-author-of-pydantic-plugin-for-pycharm/).

##  Features
### Implemented
#### pydantic.BaseModel
* Model-specific `__init__`-signature inspection and autocompletion for subclasses of `pydantic.BaseModel`
* Model-specific `__init__`-arguments type-checking for subclasses of `pydantic.BaseModel` 
* Refactor support for renaming fields for subclasses of `BaseModel`
  * (If the field name is refactored from the model definition or `__init__` call keyword arguments, PyCharm will present a dialog offering the choice to automatically rename the keyword where it occurs in a model initialization call.
* Search related-fields by class attributes and keyword arguments of `__init__` with `Ctrl+B` and `Cmd+B`
* Provide an inspection for type-checking which is compatible with pydantic. the inspection supports `parsable-type`. the detail is at [Inspection for type-checking section](https://koxudaxi.github.io/pydantic-pycharm-plugin/type-checking/)
#### pydantic.dataclasses.dataclass
* Support same features as `pydantic.BaseModel`
  * (After PyCharm 2020.1 and this plugin version 0.1.0, PyCharm treats `pydantic.dataclasses.dataclass` as third-party dataclass.)

## Example:
![type check1](typecheck1.png)

## Quick Installation
The plugin is in Jetbrains repository ([Pydantic Plugin Page](https://plugins.jetbrains.com/plugin/12861-pydantic))

You can install the stable version on PyCharm's `Marketplace` (Preference -> Plugins -> Marketplace) [Offical Document](https://www.jetbrains.com/help/idea/managing-plugins.html)

**The plugin requires PyCharm 2020.1 or later (include other JetBrains IDEs)**
 
![search plugin](search_plugin.png)


## Contribute
We are waiting for your contributions to `pydantic-pycharm-plugin`.

## Links
### JetBrains Plugin Page
[Pydantic Plugin Page](https://plugins.jetbrains.com/plugin/12861-pydantic)

### Motivation
[Auto-completion when instantiating BaseModel objects #650](https://github.com/samuelcolvin/pydantic/issues/650)

### Other PyCharm plugin projects
[Poetry PyCharm Plugin](https://github.com/koxudaxi/poetry-pycharm-plugin/)

## This project is currently in an experimental phase
