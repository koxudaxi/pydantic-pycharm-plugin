from builtins import *

from pydantic import BaseModel


class A(BaseModel):
    a: int


class B(A):
    def __init__(self, a):
        super().__init__(a=a)


B(a=int(123))
