from typing import Generic, TypeVar
from pydantic.generics import GenericModel


AT = TypeVar("AT")


class A(GenericModel, Generic[AT]):
    a: AT


class B(A[AT], Generic[AT]):
    b: int


B[str]().<caret>
