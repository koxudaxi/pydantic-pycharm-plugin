
from typing import Union

from pydantic import BaseModel


class A(BaseModel):
    a: str


A(a=str('123'))
A(<weak_warning descr="Field is of type 'str', 'int' is set as an acceptable type in pyproject.toml">a=int(123)</weak_warning>)

def get_unknown_type_value():
    raise Exception()

A(a=get_unknown_type_value())
A(<warning descr="Expected type 'str', got 'None' instead">a=None</warning>)