from builtins import *
from pydantic import BaseModel


class A(BaseModel):
    abc: str
    cde = str('abc')
    efg: str = str('abc')

class B(A):
    hij: str

a = A()
a.<caret>