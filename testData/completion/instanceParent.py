from builtins import *

from pydantic import BaseModel

class A(BaseModel):
    abc: str
    cde: str

class B(A):
    efg: str

B().<caret>