from builtins import *
from typing import Union, Optional

from pydantic import BaseModel


class A(BaseModel):
    abc: Optional[str]
    cde = str('abc')
    efg: str = str('abc')

class B(A):
    hij: str

A.<caret>