from builtins import *
from typing import Union

from pydantic import BaseModel


class A(BaseModel):
    a: str

A(a=str('123'))
A(<warning descr="Expected type 'str', got 'bytes' instead">a=bytes(123)</warning>)
