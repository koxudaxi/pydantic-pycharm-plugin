
from typing import Union

from pydantic import BaseModel


class A(BaseModel):
    a: float | int


A(<warning descr="Expected type 'float | int', got 'bytes' instead">a=bytes(123)</warning>)
A(<warning descr="Expected type 'float | int', got 'str' instead">a=str('123')</warning>)
