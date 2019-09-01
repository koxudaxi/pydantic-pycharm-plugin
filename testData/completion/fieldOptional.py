from builtins import *
from typing import Union

from pydantic import BaseModel


class A(BaseModel):
    abc: Union[str, int]
    cde = str('abc')
    efg: str = str('abc')

class B(A):
    hij: str

A.<caret>