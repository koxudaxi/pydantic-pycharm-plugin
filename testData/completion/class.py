from builtins import *
from pydantic import BaseModel

class B:
    hij: str

class A(BaseModel, B):
    abc: str
    cde = str('abc')
    efg: str = str('abc')


A.<caret>