
from typing import Union

from pydantic import BaseModel


class A(BaseModel):
    a: str
    b: str

A(a=str('123'), b=str('123'))
A(a=int(123), b=int(123))
