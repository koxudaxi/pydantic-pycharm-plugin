from builtins import *
from pydantic import BaseModel

class A(BaseModel):
    __root__: str

A().<caret>