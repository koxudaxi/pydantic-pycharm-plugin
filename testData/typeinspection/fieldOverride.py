from builtins import *

from pydantic import BaseModel


class A(BaseModel):
    a: int

class B(A):
    a: str

B(a=str(123))
