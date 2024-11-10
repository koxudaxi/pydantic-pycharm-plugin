
from pydantic import BaseModel


class A(BaseModel):
    abc: str
    cde = 'abc'
    efg: str = 'abc'

class B(A):
    hij: str

a = A
a(<caret>)