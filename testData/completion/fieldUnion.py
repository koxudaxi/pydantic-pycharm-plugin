from builtins import *
from typing import Union

from pydantic import BaseModel


class A(BaseModel):
    abc: Union[str, int]
    cde: Union[str, int] = ...
    efg: str = str('abc')

class B(A):
    hij: str

A().<caret>