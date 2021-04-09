
from typing import Union

from pydantic import BaseModel


class A(BaseModel):
    abc: Union[str, int]
    cde: Union[str, int] = ...
    efg: Union[str, int, None]


class B(A):
    hij: str

A().<caret>