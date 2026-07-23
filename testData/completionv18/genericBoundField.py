from typing import Generic, TypeVar
from pydantic.generics import GenericModel


AT = TypeVar("AT", bound=int)


class A(GenericModel, Generic[AT]):
    a: AT


class B(A[AT], Generic[AT]):
    b: str


B[int]().<caret>
