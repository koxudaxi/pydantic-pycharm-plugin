from builtins import *

from pydantic import BaseModel


class A(BaseModel):
    abc: str
    cde: str
    efg: str

class B(A):
    hij: str

A(B.hi<caret>)