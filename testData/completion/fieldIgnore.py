from builtins import *
from typing import ClassVar
from pydantic import BaseModel


class A(BaseModel):
    _abc: str = str('abc')
    __cde: str = str('abc')
    efg: ClassVar[str] = str('abc')

class B(A):
    _efg: str = str('abc')
    __hij: str = str('abc')
    klm: ClassVar[str] = str('abc')

A().<caret>