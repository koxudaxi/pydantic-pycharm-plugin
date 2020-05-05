from builtins import *

from pydantic import BaseModel

class A(BaseModel):
    abc: str

class B:
    opq: str
    xyz: str = '123'

class C(A, B):
    efg: str
    def __init__(self):
        return self.<caret>


