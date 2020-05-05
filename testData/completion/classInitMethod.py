from builtins import *

from pydantic import BaseModel

class A(BaseModel):
    abc: str

class B(A):
    efg: str
    def __init__(self):
        return self.<caret>


