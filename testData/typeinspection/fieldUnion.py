from builtins import *
from typing import Union

from pydantic import BaseModel


class A(BaseModel):
    a: Union[int, str, None]


A(a=int(123))
A(a=str('123'))
A(a=None)