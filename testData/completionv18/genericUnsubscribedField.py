from typing import Generic, TypeVar
from pydantic.generics import GenericModel


AT = TypeVar("AT")


class A(GenericModel, Generic[AT]):
    a: AT


A().<caret>
