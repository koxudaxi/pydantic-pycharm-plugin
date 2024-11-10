
from pydantic import BaseModel


class A(BaseModel):
    abc: str
    cde: str = 'abc'
    efg: str = 'abc'


def get_a(a: A):
    a.<caret>