

from pydantic import BaseModel

class Z(BaseModel):
    def __init__(self, xyz: str):
        super().__init__()
class A(Z):
    abc: str
    cde: str
    efg: str

class B(A):
    hij: str

A(<caret>)