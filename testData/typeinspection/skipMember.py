from builtins import *

from pydantic import BaseModel


class A(BaseModel):
    __init__: str


class B(BaseModel):
    __new__: str


A()
B()
