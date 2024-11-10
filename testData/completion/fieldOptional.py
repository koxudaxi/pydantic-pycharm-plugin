
from typing import Union, Optional

from pydantic import BaseModel


class A(BaseModel):
    abc: Optional[str]
    cde = 'abc'
    efg: str = 'abc'

class B(A):
    hij: str

A().<caret>