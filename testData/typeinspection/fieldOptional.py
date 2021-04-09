
from typing import Optional

from pydantic import BaseModel


class A(BaseModel):
    a: Optional[int]


A(a=int(123))
A(a=None)
