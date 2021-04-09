
from pydantic import BaseModel
from typing import Type


class A(BaseModel):
    abc: str
    cde: str = str('abc')
    efg: str = str('abc')


def get_a(a: Type[A]):
    a.<caret>