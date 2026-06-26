from typing import Any, Generic, Optional, TypeVar, Union, overload

_T_co = TypeVar("_T_co", covariant=True)

# Original signature: (unresolved ancestor classes mess with type checking for tests)
# class InstrumentedAttribute(QueryableAttribute[_T_co]):
class InstrumentedAttribute(Generic[_T_co]):
    __slots__ = ()
    inherit_cache = True
    __doc__: Optional[str]

    def __set__(self, instance: object, value: Any) -> None: ...
    def __delete__(self, instance: object) -> None: ...
    @overload
    def __get__(self, instance: None, owner: Any) -> "InstrumentedAttribute[_T_co]": ...
    @overload
    def __get__(self, instance: object, owner: Any) -> _T_co: ...
    def __get__(self, instance: Optional[object], owner: Any) -> Union["InstrumentedAttribute[_T_co]", _T_co]: ...
