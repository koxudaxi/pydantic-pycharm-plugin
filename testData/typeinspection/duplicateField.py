from builtins import *

from pydantic import BaseModel

class A(BaseModel):
    a: str
    a: int

A(a=int(123))
