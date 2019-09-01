from builtins import *
from pydantic import BaseModel


class A:
    abc: str
    cde = str('abc')
    efg: str = str('abc')

class B(A):
    hij: str

a = A()
a.<caret>