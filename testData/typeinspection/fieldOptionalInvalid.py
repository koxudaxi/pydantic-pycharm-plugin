from builtins import *
from typing import Optional

from pydantic import BaseModel


class A(BaseModel):
    a: Optional[int]


A(<warning descr="Expected type 'Optional[int]', got 'str' instead">a=str('123')</warning>)
