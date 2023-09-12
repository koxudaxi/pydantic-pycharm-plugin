

from pydantic import BaseModel

class Z(BaseModel):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
class A(Z):
    abc: str
    cde: str
    efg: str

class B(A):
    hij: str

A(<caret>)