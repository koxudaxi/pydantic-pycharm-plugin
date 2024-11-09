
from pydantic import BaseModel

class B:
    hij: str

class A(BaseModel, B):
    abc: str
    cde = 'abc'
    efg: str = 'abc'


A.<caret>