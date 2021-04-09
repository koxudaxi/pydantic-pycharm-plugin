
from typing import Union

from pydantic import BaseModel


class A(BaseModel):
    a: str


A(a=str('123'))
A(<weak_warning descr="Field is of type 'str', 'int' is set as an acceptable type in pyproject.toml">a=int(123)</weak_warning>)
