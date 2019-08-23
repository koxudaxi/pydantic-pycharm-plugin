from builtins import *

from pydantic import BaseModel


class A(BaseModel):
    b

A(b=int(123))
