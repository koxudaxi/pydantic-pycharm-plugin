from typing import TYPE_CHECKING, Any, Callable, Dict, Optional, Type, TypeVar, Union, overload

if TYPE_CHECKING:
    from .main import BaseConfig, BaseModel  # noqa: F401

    DataclassT = TypeVar('DataclassT', bound='Dataclass')

    class Dataclass:
        __pydantic_model__: Type[BaseModel]
        __initialised__: bool
        __post_init_original__: Optional[Callable[..., None]]

        def __init__(self, *args: Any, **kwargs: Any) -> None:
            pass

        def __call__(self: 'DataclassT', *args: Any, **kwargs: Any) -> 'DataclassT':
            pass


@overload
def dataclass(
        *,
        init: bool = True,
        repr: bool = True,
        eq: bool = True,
        order: bool = False,
        unsafe_hash: bool = False,
        frozen: bool = False,
        config: Type[Any] = None,
) -> Callable[[Type[Any]], Type['Dataclass']]:
    ...


@overload
def dataclass(
        _cls: Type[Any],
        *,
        init: bool = True,
        repr: bool = True,
        eq: bool = True,
        order: bool = False,
        unsafe_hash: bool = False,
        frozen: bool = False,
        config: Type[Any] = None,
) -> Type['Dataclass']:
    ...


def dataclass(
        _cls: Optional[Type[Any]] = None,
        *,
        init: bool = True,
        repr: bool = True,
        eq: bool = True,
        order: bool = False,
        unsafe_hash: bool = False,
        frozen: bool = False,
        config: Type[Any] = None,
) -> Union[Callable[[Type[Any]], Type['Dataclass']], Type['Dataclass']]:

    def wrap(cls: Type[Any]) -> Type['Dataclass']:
        pass
    if _cls is None:
        return wrap

    return wrap(_cls)
