from builtins import *
from typing import Union

from pydantic import BaseModel


class A(BaseModel):
    a: str


A(a=str('123'))
A(<weak_warning descr="Field is of type 'str', 'int' may not be parsable to 'str'">a=int(123)</weak_warning>)
