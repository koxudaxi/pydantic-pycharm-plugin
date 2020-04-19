from builtins import *
from typing import Union

from pydantic import BaseModel


class A(BaseModel):
    a: str


A(a=str('123'))
A(a=int(123))
