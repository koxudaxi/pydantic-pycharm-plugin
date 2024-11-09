
from pydantic import BaseModel
from typing import List, Type

class A(BaseModel):
    a<caret>bc: str
    cde = 'abc'
    efg: str = 'abc'

class B(A):
    hij: str

def get_a(a: Type[A]):
    return a(abc=1)