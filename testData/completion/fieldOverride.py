from builtins import *

from pydantic import BaseModel


class A(BaseModel):
    a: int

class B(A):
    a: str

b = B()
b.<caret>