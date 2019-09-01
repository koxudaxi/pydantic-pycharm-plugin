from builtins import *
from pydantic import BaseModel, Schema

class A(BaseModel):
    abc: str = Schema(...)
    cde = Schema(str('abc'))
    efg = Schema(default=str('abc'))
    hij = Schema(default=...)

class B(A):
    hij: str

A().<caret>