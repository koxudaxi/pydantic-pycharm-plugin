from __future__ import annotations as _annotations

import typing
import warnings
from abc import ABCMeta
from copy import copy, deepcopy
from functools import partial
from inspect import getdoc
from pathlib import Path
from types import prepare_class, resolve_bases
from typing import Any, Generic

import pydantic_core
import typing_extensions

from ._internal import (
    _decorators,
    _forward_ref,
    _generics,
    _model_construction,
    _repr,
    _typing_extra,
    _utils,
)
from ._internal._fields import Undefined
from .analyzed_type import AnalyzedType
from .config import BaseConfig, ConfigDict, Extra, build_config, get_config
from .deprecated import copy_internals as _deprecated_copy_internals
from .deprecated import parse as _deprecated_parse
from .errors import PydanticUndefinedAnnotation, PydanticUserError
from .fields import Field, FieldInfo, ModelPrivateAttr
from .json import custom_pydantic_encoder, pydantic_encoder
from .json_schema import DEFAULT_REF_TEMPLATE, GenerateJsonSchema, JsonSchemaMetadata

if typing.TYPE_CHECKING:
    from inspect import Signature

    from pydantic_core import CoreSchema, SchemaSerializer, SchemaValidator

    from ._internal._generate_schema import GenerateSchema
    from ._internal._utils import AbstractSetIntStr, MappingIntStrAny

    AnyClassMethod = classmethod[Any]
    TupleGenerator = typing.Generator[tuple[str, Any], None, None]
    Model = typing.TypeVar('Model', bound='BaseModel')
    # should be `set[int] | set[str] | dict[int, IncEx] | dict[str, IncEx] | None`, but mypy can't cope
    IncEx = set[int] | set[str] | dict[int, Any] | dict[str, Any] | None

__all__ = 'BaseModel', 'create_model'

_object_setattr = _model_construction.object_setattr
# Note `ModelMetaclass` refers to `BaseModel`, but is also used to *create* `BaseModel`, so we need to add this extra
# (somewhat hacky) boolean to keep track of whether we've created the `BaseModel` class yet, and therefore whether it's
# safe to refer to it. If it *hasn't* been created, we assume that the `__new__` call we're in the middle of is for
# the `BaseModel` class, since that's defined immediately after the metaclass.
_base_class_defined = False


@typing_extensions.dataclass_transform(kw_only_default=True, field_specifiers=(Field,))
class ModelMetaclass(ABCMeta):
    def __new__(  # noqa C901
        mcs,
        cls_name: str,
        bases: tuple[type[Any], ...],
        namespace: dict[str, Any],
        __pydantic_generic_origin__: type[BaseModel] | None = None,
        __pydantic_generic_args__: tuple[Any, ...] | None = None,
        __pydantic_generic_parameters__: tuple[Any, ...] | None = None,
        __pydantic_reset_parent_namespace__: bool = True,
        **kwargs: Any,
    ) -> type:
        ...
    def __instancecheck__(self, instance: Any) -> bool:
        ...
class BaseModel(_repr.Representation, metaclass=ModelMetaclass):
    if typing.TYPE_CHECKING:
        # populated by the metaclass, defined here to help IDEs only
        __pydantic_validator__: typing.ClassVar[SchemaValidator]
        __pydantic_core_schema__: typing.ClassVar[CoreSchema]
        __pydantic_serializer__: typing.ClassVar[SchemaSerializer]
        __pydantic_decorators__: typing.ClassVar[_decorators.DecoratorInfos]
        """metadata for `@validator`, `@root_validator` and `@serializer` decorators"""
        model_fields: typing.ClassVar[dict[str, FieldInfo]] = {}
        __json_encoder__: typing.ClassVar[typing.Callable[[Any], Any]] = lambda x: x  # noqa: E731
        __signature__: typing.ClassVar[Signature]
        __private_attributes__: typing.ClassVar[dict[str, ModelPrivateAttr]]
        __class_vars__: typing.ClassVar[set[str]]
        __fields_set__: set[str] = set()
        __pydantic_generic_args__: typing.ClassVar[tuple[Any, ...] | None]
        __pydantic_generic_defaults__: typing.ClassVar[dict[str, Any] | None]
        __pydantic_generic_origin__: typing.ClassVar[type[BaseModel] | None]
        __pydantic_generic_parameters__: typing.ClassVar[tuple[_typing_extra.TypeVarType, ...] | None]
        __pydantic_generic_typevars_map__: typing.ClassVar[dict[_typing_extra.TypeVarType, Any] | None]
        __pydantic_parent_namespace__: typing.ClassVar[dict[str, Any] | None]


    model_config = ConfigDict()
    __slots__ = '__dict__', '__fields_set__'
    __doc__ = ''  # Null out the Representation docstring
    __pydantic_model_complete__ = False

    def __init__(__pydantic_self__, **data: Any) -> None:
        ...
    @classmethod
    def __get_pydantic_core_schema__(cls, source: type[BaseModel], gen_schema: GenerateSchema) -> CoreSchema:
        ...
    @classmethod
    def model_validate(cls: type[Model], obj: Any) -> Model:
        ...
    @classmethod
    def model_validate_json(cls: type[Model], json_data: str | bytes | bytearray) -> Model:
        ...

    if typing.TYPE_CHECKING:
        # model_after_init is called after at the end of `__init__` if it's defined
        def model_post_init(self, _context: Any) -> None:
            pass

    def __setattr__(self, name: str, value: Any) -> None:
        ...
    def __getstate__(self) -> dict[Any, Any]:
        ...

    def __setstate__(self, state: dict[Any, Any]) -> None:
        ...

    def model_dump(
        self,
        *,
        mode: typing_extensions.Literal['json', 'python'] | str = 'python',
        include: IncEx = None,
        exclude: IncEx = None,
        by_alias: bool = False,
        exclude_unset: bool = False,
        exclude_defaults: bool = False,
        exclude_none: bool = False,
        round_trip: bool = False,
        warnings: bool = True,
    ) -> dict[str, Any]:
        ...

    def model_dump_json(
        self,
        *,
        indent: int | None = None,
        include: IncEx = None,
        exclude: IncEx = None,
        by_alias: bool = False,
        exclude_unset: bool = False,
        exclude_defaults: bool = False,
        exclude_none: bool = False,
        round_trip: bool = False,
        warnings: bool = True,
    ) -> str:
        ...

    @classmethod
    def model_construct(cls: type[Model], _fields_set: set[str] | None = None, **values: Any) -> Model:
        ...

    @classmethod
    def model_json_schema(
        cls,
        by_alias: bool = True,
        ref_template: str = DEFAULT_REF_TEMPLATE,
        schema_generator: type[GenerateJsonSchema] = GenerateJsonSchema,
    ) -> dict[str, Any]:
        ...

    @classmethod
    def model_json_schema_metadata(cls) -> JsonSchemaMetadata | None:
        ...
    @classmethod
    def model_rebuild(
        cls,
        *,
        force: bool = False,
        raise_errors: bool = True,
        _parent_namespace_depth: int = 2,
    ) -> bool | None:
        ...
    def __iter__(self) -> TupleGenerator:
        ...
    def __eq__(self, other: Any) -> bool:
        ...
    def model_copy(self: Model, *, update: dict[str, Any] | None = None, deep: bool = False) -> Model:
        ...
    def __copy__(self: Model) -> Model:
        ...
    def __deepcopy__(self: Model, memo: dict[int, Any] | None = None) -> Model:
        ...
    def __repr_args__(self) -> _repr.ReprArgs:
        ...

    def __class_getitem__(
        cls, typevar_values: type[Any] | tuple[type[Any], ...]
    ) -> type[BaseModel] | _forward_ref.PydanticForwardRef | _forward_ref.PydanticRecursiveRef:
        ...
    @classmethod
    def model_parametrized_name(cls, params: tuple[type[Any], ...]) -> str:
        ...
    # ##### Deprecated methods from v1 #####
    def dict(
        self,
        *,
        include: IncEx = None,
        exclude: IncEx = None,
        by_alias: bool = False,
        exclude_unset: bool = False,
        exclude_defaults: bool = False,
        exclude_none: bool = False,
    ) -> typing.Dict[str, Any]:  # noqa UP006
        ...

    def json(
        self,
        *,
        include: IncEx = None,
        exclude: IncEx = None,
        by_alias: bool = False,
        exclude_unset: bool = False,
        exclude_defaults: bool = False,
        exclude_none: bool = False,
        # TODO: What do we do about the following arguments?
        #   Do they need to go on model_config now, and get used by the serializer?
        encoder: typing.Callable[[Any], Any] | None = Undefined,  # type: ignore[assignment]
        models_as_dict: bool = Undefined,  # type: ignore[assignment]
        **dumps_kwargs: Any,
    ) -> str:
        ...
    @classmethod
    def parse_obj(cls: type[Model], obj: Any) -> Model:
        ...
    @classmethod
    def parse_raw(
        cls: type[Model],
        b: str | bytes,
        *,
        content_type: str = None,
        encoding: str = 'utf8',
        proto: _deprecated_parse.Protocol = None,
        allow_pickle: bool = False,
    ) -> Model:
        ...
    @classmethod
    def parse_file(
        cls: type[Model],
        path: str | Path,
        *,
        content_type: str = None,
        encoding: str = 'utf8',
        proto: _deprecated_parse.Protocol = None,
        allow_pickle: bool = False,
    ) -> Model:
        ...
    @classmethod
    def from_orm(cls: type[Model], obj: Any) -> Model:
        warnings.warn(
            'The `from_orm` method is deprecated; set model_config["from_attributes"]=True '
            'and use `model_validate` instead.',
            DeprecationWarning,
        )
        ...
    @classmethod
    def construct(cls: type[Model], _fields_set: set[str] | None = None, **values: Any) -> Model:
        ...
    def copy(
        self: Model,
        *,
        include: AbstractSetIntStr | MappingIntStrAny | None = None,
        exclude: AbstractSetIntStr | MappingIntStrAny | None = None,
        update: typing.Dict[str, Any] | None = None,  # noqa UP006
        deep: bool = False,
    ) -> Model:
        ...
    @classmethod
    def schema(
        cls, by_alias: bool = True, ref_template: str = DEFAULT_REF_TEMPLATE
    ) -> typing.Dict[str, Any]:  # noqa UP006
        ...
    @classmethod
    def schema_json(
        cls, *, by_alias: bool = True, ref_template: str = DEFAULT_REF_TEMPLATE, **dumps_kwargs: Any
    ) -> str:
        ...
    @classmethod
    def validate(cls: type[Model], value: Any) -> Model:
        ...
    @classmethod
    def update_forward_refs(cls, **localns: Any) -> None:
        ...
    def _iter(self, *args: Any, **kwargs: Any) -> Any:
        ...
    def _copy_and_set_values(self, *args: Any, **kwargs: Any) -> Any:
        ...
    @classmethod
    def _get_value(cls, *args: Any, **kwargs: Any) -> Any:
        ...
    def _calculate_keys(self, *args: Any, **kwargs: Any) -> Any:
        ...

_base_class_defined = True


@typing.overload
def create_model(
    __model_name: str,
    *,
    __config__: ConfigDict | type[BaseConfig] | None = None,
    __base__: None = None,
    __module__: str = __name__,
    __validators__: dict[str, AnyClassMethod] = None,
    __cls_kwargs__: dict[str, Any] = None,
    **field_definitions: Any,
) -> type[Model]:
    ...


@typing.overload
def create_model(
    __model_name: str,
    *,
    __config__: ConfigDict | type[BaseConfig] | None = None,
    __base__: type[Model] | tuple[type[Model], ...],
    __module__: str = __name__,
    __validators__: dict[str, AnyClassMethod] = None,
    __cls_kwargs__: dict[str, Any] = None,
    **field_definitions: Any,
) -> type[Model]:
    ...


def create_model(
    __model_name: str,
    *,
    __config__: ConfigDict | type[BaseConfig] | None = None,
    __base__: type[Model] | tuple[type[Model], ...] | None = None,
    __module__: str = __name__,
    __validators__: dict[str, AnyClassMethod] = None,
    __cls_kwargs__: dict[str, Any] = None,
    __slots__: tuple[str, ...] | None = None,
    **field_definitions: Any,
) -> type[Model]:
    ...

def _collect_bases_data(bases: tuple[type[Any], ...]) -> tuple[set[str], set[str], dict[str, ModelPrivateAttr]]:
    ...