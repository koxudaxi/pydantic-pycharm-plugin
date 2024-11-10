
from pydantic import BaseModel


class A:
    abc: str
    cde = 'abc'
    efg: str = 'abc'

class B(A):
    hij: str

a = A()
a.<caret>