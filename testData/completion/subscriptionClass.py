
from pydantic import BaseModel
from typing import List, Type

class A(BaseModel):
    abc: str
    cde = 'abc'
    efg: str = 'abc'

class B(A):
    hij: str

def get_a(a_list: List[Type[A]]):
    return a_list[0](<caret>)