from builtins import *

from pydantic import BaseModel, Schema


class A(BaseModel):
    a: int = Schema(int(123))
    b = Schema(123)
    c = Schema(default=int(123))
    d: int = Schema(...)

A(a=int(123), b=int(123), c=int(123), d=int(123))
