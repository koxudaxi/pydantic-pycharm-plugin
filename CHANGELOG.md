# Changelog

## [Unreleased]

## [0.4.5] - 2023-07-17
- Add migration guide url for 231 [[#750](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/750)]
- Update v2 external document url for 231 [[#748](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/748)]
- Support ConfigDict for 231 [[#738](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/738)]

## [0.4.4] - 2023-07-14
- Support pydantic v2 validators for 231 [[#736](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/736)]

## [0.4.3] - 2023-06-26
- Add error message for root model in v2 [[#696](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/686)]
- Add official document link [[#697](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/697)]

## [0.4.2-231] - 2023-03-22
- Bump version to 0.4.2-231 [[#686](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/686)]
- Fix wrong accepts only keyword arguments error [[#671](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/671)]
- Support # noqa [[#680](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/680)]
- Fix wrong renaming of a local variable name is same as field name [[#681](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/681)]
- Fix wrong highlighting [[#682](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/682)]

## [0.4.2] - 2023-03-21
- Fix wrong accepts only keyword arguments error [[#671](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/671)]
- Support # noqa [[#680](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/680)]
- Fix wrong renaming of a local variable name is same as field name [[#681](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/681)]
- Fix wrong highlighting [[#682](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/682)]

## [0.4.1-231] - 2023-03-02
- Support 2023.1 EAP [[#633](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/633)]

## [0.4.0] - 2023-03-02
- Fix wrong inspections when a model has a __call__ method [[#655](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/655)]
- Reduce unnecessary resolve in type providers [[#656](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/656)]
- Optimize resolving pydantic class [[#658](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/658)]
- Improve dynamic model field detection [[#659](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/659)]
- Improve test coverage [[#660](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/660)]
- Use multiResolveCalleeFunction instead of getResolvedPsiElements [[#661](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/661)]

## [0.3.17] - 2022-12-16
- Support Union operator [[#602](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/602)]
- Ignore forbid for double star arguments [[#603](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/603)]
- Improve dataclass default Value detection [[#604](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/604)]
- Add inspection for default factory [[#605](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/605)]
- Improve insert arguments [[#607](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/607)]
- Fix None default value on Field function[[#608](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/608)]
- Improve dataclass support [[#609](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/609)]

## [0.3.16] - 2022-12-09
- avoid AlreadyDisposedException [[#585](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/585)]
- Fix kotlin jvm target option [[#586](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/586)]
- Improve code style [[#587](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/587)]
- Prevent Recursion problem [[#594](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/594)]
- Improve version management [[#595](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/595)]
- Fix completion adds superfluous equal to field_name [[#596](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/596)]

## [0.3.15]
- Fix NotFound getDataclassParameters error [[#573](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/573)]

## [0.3.14]
- Support IntelliJ IDEA 2022.3 [[#519](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/519)]

## [0.3.13]

### Features
- Support IntelliJ IDEA 2022.2.2 [[#517](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/517)]

## [0.3.12]

### Features
- Support SQLModel [[#450](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/450)]

## [0.3.11]

### Features
- Support IntelliJ IDEA 2022.1 [[#436](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull/436)]

### BugFixes
- Fix Null Pointer Exception in PydanticTypeCheckerInspection [[#431](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//431)]

## [0.3.10]

### Features
- Support IntelliJ IDEA 2021.3 [[#407](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//407)]

### BugFixes
- Fix a typo in the settings [[#408](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//408)]

## [0.3.9]

### Features
- Support PyCharm 2021.3 [[#400](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//400)]

## [0.3.8]

### Features
- PyCharm API changes [[#350](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//350)] by @alek-sun
- Thanks to @alek-sun

## [0.3.7]

### BugFixes
- Improve resolving ancestor pydantic models [[#369](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//369)]
- Fix false positive detection of "extra fields not permitted" [[#368](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//368)]

## [0.3.6]

### BugFixes
- Fix PydanticDataclassTypeProvider.kt error [[#366](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//366)]
- Fix Outdated Stub in index error on PydanticAnnotator.kt [[#363](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//363)]
- Fix NullPointerException in PydanticTypeCheckerInspection.kt [[#362](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//362)]

## [0.3.5]

### Features
- Support PyCharm 2021.2 [[#355](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//355)]
- PyCharm 2021.2.1 API changes [[#345](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//345)] by @lada-gagina
- Thanks to @lada-gagina

## [0.3.4]

### Features
- Support ignore-init-method-arguments [[#328](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//328)]
- Support error for extra attribute with extra = 'forbid' option [[#324](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//324)]

### BugFixes
- Fix default value by variable for Field is not recognized [[#323](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//323)]

## [0.3.3]

### BugFixes
- Ignore invalid alias name [[#307](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//307)]
- Fix wrong call parameter with **kwargs [[#306](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//306)]

## [0.3.2]

### BugFixes
- Fix wrong call parameters when init is defined [[#298](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//298)]
- Fix wrong an error for a duplicate in config [[#297](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//297)]

## [0.3.1]

### Features
- Improve resolving reference [[#293](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//293)]
- Improve coding style [[#292](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//292)]
- Support GenericModel [[#289](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//289)]
- Support frozen on config [[#288](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//288)]
- Fix format [[#287](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//287)]
- Improve handling pydantic version [[#286](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//286)]
- Support config parameters on class kwargs [[#285](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//285)]

## [0.3.0]

### Features
- Support extra init args on baseSetting [[#276](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//276)]
- Support PyCharm 2021.1 [[#273](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//273)]
- Improve supporting dynamic model [[#271](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//271)]

## [0.2.1]

### Features
- Support regex (Field, constr) [[#262](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//262)]

## [0.2.0]

### BugFixes
- Support `import typing` [[#258](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//258)]
- Fix DisposalException [[#252](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//252)]
- Support Annotated [[#241](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//241)]

## [0.1.20]

### Features
- Show Field() as parameter info for a default value when set default_factory [[#240](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//240)]

## [0.1.19]

### BugFixes
- Fix custom root inspection [[#232](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//232)]

## [0.1.18]

### Features
- Support custom root field [[#227](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//227)]

## [0.1.17]

### Features
- Support keep_untouched[[#216](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//216)]

### BugFixes
- Fix build warning [[#217](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//217)]

## [0.1.16]

### BugFixes
- Fix inserting argument [[#204](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//204)]

## [0.1.15]

### BugFixes
- Fix config service error [[#202](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//202)]

## [0.1.14]

### BugFixes
- Fix detecting validators decorated methods [[#196](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//196)]
- Remove stub deletion error [[#190](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//190)]

## [0.1.13]

### Features
- Support ClassVar [[#188](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//188)]

## [0.1.12]

### Features
- Improve build config [[#180](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//180)]

## [0.1.11]

### Features
- Support dynamic model [[#175](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//175)]

## [0.1.10]

### BugFixes
- Fix inserting arguments [[#160](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//160)]

## [0.1.9]

### BugFixes
- Fix compatibility issues [[#145](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//145)]

## [0.1.8]

### Features
- Support inserting arguments [[#144](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//144)]

## [0.1.7]

### Features
- Update jvm version [[#133](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//133)]

### BugFixes
- Fix handling project [[#137](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//137)]
- Fix invalid cache for pydantic version [[#132](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//132)]
- Fix invalid completion in callable expression [[#130](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//130)]

## [0.1.6]

### Features
- Support conlist [[#129](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//129)]
- Fix acceptable types for collections [[#127](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//127)]
- Improve initializer and add package manager listener [[#126](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//126)]
- Fix invalid self parameter when inherits from non-pydantic model [[#125](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//125)]
- Add mock sdk for unittest [[#124](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//124)]
- Fix types of methods and functions [[#123](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//123)]

## [0.1.5]

### Features
- Support a collection on parsable-type and acceptable-type [[#120](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//120)]

### BugFixes
- Fix an error when project is disposed [[#121](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//121)]
- Fix type-map edge case for parsable-type and acceptable-type [[#118](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//118)]

## [0.1.4]

### BugFixes
- Fix type provider for dataclass [[#114](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//114)]

### Features
- Support mypy.ini [[#110](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//110)]

## [0.1.3]

### Features
- Add documents and link to documents [[#105](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//105), [#106](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//106), [#107](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//107), [#108](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//108)]
- Support acceptable type [[#104](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//104)]
- Support parsable type highlight level [[#103](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//103)]

## [0.1.2]

### BugFixes
- Fix type checker [[#102](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//102)]
- Fix an invalid warning when a field type is any [[#101](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//101)]
- Fix plugin build settings [[#100](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//100)]

## [0.1.1]

### Features
- Support parsable type [[#96](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//96)]

## [0.1.0]

### Features
- PyCharm treats pydantic.dataclasses.dataclass as third-party dataclass. [[#98](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//98)]

## [0.0.30]

### BugFixes
- Fix invalid warn on no public attribute [[#95](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//95)]

## [0.0.29]

### Features
- Inspect untyped fields [[#93](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//93)]
- Add config panel [[#92](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//92)]

## [0.0.28]

### Features, BugFixes
- Support positional arguments for dataclasses [[#91](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//91)]
- Fix field names treated with incorrect scope [[#90](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//90)]

## [0.0.27]

### Features
- Support to inspect read-only property [[#86](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//86)]

## [0.0.26]

### Features
- Support to inspect from_orm [[#85](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//85)]
- Improve to handle Config  [[#85](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//85)]

## [0.0.25]

### Features
- Add auto-completion for config fields [[#84](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//84)]
- Support allow_population_by_field_name [[#82](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//82)]

## [0.0.24]

### BugFixes
- Fix inspection on namedtuple [[#81](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//81)]

## [0.0.23]

### Features
- Ignore protected and private fields [[#79](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//79)]

## [0.0.22]

### Features, BugFixes
- Fix first parameter type of a validator method [[#76](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//76)]
- Fix auto-completion for Fields [[#75](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//75)]
- Improve to insert validate methods [[#74](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//74)]

## [0.0.21]

### Features, BugFixes
- Support root_validator [[#72](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//72)]
- Support Field for v1 [[#71](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//71), [#73](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//73)]

## [0.0.20]

### Features, BugFixes
- Support all features by parameters [[#67](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//67)]
- Fix to handle models which have __init__ or __new__ methods [[#67](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//67)]

## [0.0.19]

### BugFixes
- Fix wrong warning message for cls initialization [[#66](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//66)]

## [0.0.18]

### Features
- Support alias on Schema [[#64](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//64)]

## [0.0.17]

### BugFixes
- Fix removing fields on non-subclasses of pydantic.BaseModel and pydantic.dataclasses.dataclass [[#62](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//62)]

## [0.0.16]

### Features, BugFixes
- Remove fields on auto-completion of subclasses of pydantic.BaseModel and pydantic.dataclasses.dataclass [[#61](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//61)]
- Change default value "..." to None on auto-completion [[#60](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//60)]
- Add types and default values to popup of auto-completion [[#54](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//54)]
- Fix class imported path on auto-completion [[#54](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//54)]

## [0.0.15]

### Features
- Improve autocompletion for signature subclasses of pydantic.BaseModel and pydantic.dataclasses.dataclass [[#51](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//51)]
- Update kotlin version to 1.3.50  [[#50](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//50)]
- Support to detect types by default value on Schema [[#49](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//49)]
- Improve inner logic [[#47](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//47), [#52](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//52)]

## [0.0.14]

### Features
- Support default values [[#46](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//46)]
- Ignore warning for self argument with @validator [[#45](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//45)]
- Support pydantic.dataclasses.dataclass [[#43](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//43)]
- Search related-fields by class attributes and keyword arguments of __init__. with Ctrl+B and Cmd+B [[#42](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//42)]

## [0.0.13]

### Features, BugFixes
- Fix to check a type of fields without a type-hint [[#39](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//39)]
- No arguments required for BaseSettings [[#38](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//38)]

## [0.0.12]

### Features
- Support refactoring fields by a keyword argument [[#34](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//34)]
- Support refactoring super-classes and inheritor-classes [[#34](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//34)]
- Support ellipsis(...) in fields [[#34](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//34)]
- Support Schema in fields [[#31](https://github.com/koxudaxi/pydantic-pycharm-plugin/pull//31)]

[Unreleased]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.4.5...HEAD
[0.4.5]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.4.4...v0.4.5
[0.4.4]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.4.3...v0.4.4
[0.4.3]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.4.2-231...v0.4.3
[0.4.2]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.4.1-231...v0.4.2
[0.4.2-231]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.4.2...v0.4.2-231
[0.4.1-231]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.4.0...v0.4.1-231
[0.4.0]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.17...v0.4.0
[0.3.17]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.16...v0.3.17
[0.3.16]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.15...v0.3.16
[0.3.15]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.14...v0.3.15
[0.3.14]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.13...v0.3.14
[0.3.13]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.12...v0.3.13
[0.3.12]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.11...v0.3.12
[0.3.11]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.10...v0.3.11
[0.3.10]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.9...v0.3.10
[0.3.9]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.8...v0.3.9
[0.3.8]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.7...v0.3.8
[0.3.7]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.6...v0.3.7
[0.3.6]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.5...v0.3.6
[0.3.5]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.4...v0.3.5
[0.3.4]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.3...v0.3.4
[0.3.3]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.2...v0.3.3
[0.3.2]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.1...v0.3.2
[0.3.1]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.3.0...v0.3.1
[0.3.0]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.2.1...v0.3.0
[0.2.1]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.20...v0.2.0
[0.1.20]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.19...v0.1.20
[0.1.19]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.18...v0.1.19
[0.1.18]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.17...v0.1.18
[0.1.17]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.16...v0.1.17
[0.1.16]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.15...v0.1.16
[0.1.15]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.14...v0.1.15
[0.1.14]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.13...v0.1.14
[0.1.13]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.12...v0.1.13
[0.1.12]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.11...v0.1.12
[0.1.11]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.10...v0.1.11
[0.1.10]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.9...v0.1.10
[0.1.9]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.8...v0.1.9
[0.1.8]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.7...v0.1.8
[0.1.7]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.6...v0.1.7
[0.1.6]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.5...v0.1.6
[0.1.5]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.4...v0.1.5
[0.1.4]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.3...v0.1.4
[0.1.3]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.2...v0.1.3
[0.1.2]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.30...v0.1.0
[0.0.30]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.29...v0.0.30
[0.0.29]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.28...v0.0.29
[0.0.28]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.27...v0.0.28
[0.0.27]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.26...v0.0.27
[0.0.26]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.25...v0.0.26
[0.0.25]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.24...v0.0.25
[0.0.24]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.23...v0.0.24
[0.0.23]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.22...v0.0.23
[0.0.22]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.21...v0.0.22
[0.0.21]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.20...v0.0.21
[0.0.20]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.19...v0.0.20
[0.0.19]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.18...v0.0.19
[0.0.18]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.17...v0.0.18
[0.0.17]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.16...v0.0.17
[0.0.16]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.15...v0.0.16
[0.0.15]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.14...v0.0.15
[0.0.14]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.13...v0.0.14
[0.0.13]: https://github.com/koxudaxi/pydantic-pycharm-plugin/compare/v0.0.12...v0.0.13
[0.0.12]: https://github.com/koxudaxi/pydantic-pycharm-plugin/commits/v0.0.12
