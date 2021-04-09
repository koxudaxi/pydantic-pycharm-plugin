
from pydantic import BaseModel


class A(BaseModel):
    abc: str
    cde: str = str('abc')
    efg: str = str('abc')


def get_a(a = A()):
    a.<caret>