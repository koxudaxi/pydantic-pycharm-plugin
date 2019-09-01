from builtins import *
from pydantic import BaseModel


class A(BaseModel):
    abc: str
    cde = str('abc')
    efg: str = ...

class B(A):
    hij: str

A.<caret>