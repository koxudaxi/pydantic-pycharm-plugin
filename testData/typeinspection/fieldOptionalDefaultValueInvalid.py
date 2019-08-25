from builtins import *
from typing import Optional

from pydantic import BaseModel


class A(BaseModel):
    a = int(123)


A(<warning descr="Expected type 'int', got 'str' instead">a=str('123')</warning>)
