
from pydantic import BaseModel


class A(BaseModel):
    abc: str
    cde = 'abc'
    efg: str = ...
    hij = ...

class B(A):
    hij: str

A().<caret>