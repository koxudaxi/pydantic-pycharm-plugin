from typing import *

def Field(
        default: Any = Undefined,
        *,
        default_factory: Optional[NoArgAnyCallable] = None,
        alias: Optional[str] = None,
        title: Optional[str] = None,
        description: Optional[str] = None,
        exclude: Union[
            AbstractSet[Union[int, str]], Mapping[Union[int, str], Any], Any
        ] = None,
        include: Union[
            AbstractSet[Union[int, str]], Mapping[Union[int, str], Any], Any
        ] = None,
        const: Optional[bool] = None,
        gt: Optional[float] = None,
        ge: Optional[float] = None,
        lt: Optional[float] = None,
        le: Optional[float] = None,
        multiple_of: Optional[float] = None,
        min_items: Optional[int] = None,
        max_items: Optional[int] = None,
        min_length: Optional[int] = None,
        max_length: Optional[int] = None,
        allow_mutation: bool = True,
        regex: Optional[str] = None,
        primary_key: bool = False,
        foreign_key: Optional[Any] = None,
        nullable: Union[bool, UndefinedType] = Undefined,
        index: Union[bool, UndefinedType] = Undefined,
        sa_column: Union[Column, UndefinedType] = Undefined,  # type: ignore
        sa_column_args: Union[Sequence[Any], UndefinedType] = Undefined,
        sa_column_kwargs: Union[Mapping[str, Any], UndefinedType] = Undefined,
        schema_extra: Optional[Dict[str, Any]] = None,
) -> Any:
    ...

def __dataclass_transform__(
        *,
        eq_default: bool = True,
        order_default: bool = False,
        kw_only_default: bool = False,
        field_descriptors: Tuple[Union[type, Callable[..., Any]], ...] = (()),
) -> Callable[[_T], _T]:
    return lambda a: a


class FieldInfo(PydanticFieldInfo):
    def __init__(self, default: Any = Undefined, **kwargs: Any) -> None:
        ...

@__dataclass_transform__(kw_only_default=True, field_descriptors=(Field, FieldInfo))
class SQLModelMetaclass(ModelMetaclass, DeclarativeMeta):
    __sqlmodel_relationships__: Dict[str, RelationshipInfo]
    __config__: Type[BaseConfig]
    __fields__: Dict[str, ModelField]

    # Replicate SQLAlchemy
    def __setattr__(cls, name: str, value: Any) -> None:
        if getattr(cls.__config__, "table", False):
            DeclarativeMeta.__setattr__(cls, name, value)
        else:
            super().__setattr__(name, value)

    def __delattr__(cls, name: str) -> None:
        if getattr(cls.__config__, "table", False):
            DeclarativeMeta.__delattr__(cls, name)
        else:
            super().__delattr__(name)

    # From Pydantic
    def __new__(
            cls,
            name: str,
            bases: Tuple[Type[Any], ...],
            class_dict: Dict[str, Any],
            **kwargs: Any,
    ) -> Any:
        ...

    def __init__(
            cls, classname: str, bases: Tuple[type, ...], dict_: Dict[str, Any], **kw: Any
    ) -> None:
        ...


_TSQLModel = TypeVar("_TSQLModel", bound="SQLModel")


class SQLModel(BaseModel, metaclass=SQLModelMetaclass, registry=default_registry):
    # SQLAlchemy needs to set weakref(s), Pydantic will set the other slots values
    __slots__ = ("__weakref__",)
    __tablename__: ClassVar[Union[str, Callable[..., str]]]
    __sqlmodel_relationships__: ClassVar[Dict[str, RelationshipProperty]]  # type: ignore
    __name__: ClassVar[str]
    metadata: ClassVar[MetaData]

    class Config:
        orm_mode = True

    def __new__(cls, *args: Any, **kwargs: Any) -> Any:
        ...

    def __init__(__pydantic_self__, **data: Any) -> None:
        ...