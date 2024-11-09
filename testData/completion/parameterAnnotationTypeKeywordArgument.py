
from pydantic import BaseModel
from typing import Type


class A(BaseModel):
    abc: str
    cde: str = 'abc'
    efg: str = 'abc'


def get_a(a: Type[A]):
    a(<caret>)