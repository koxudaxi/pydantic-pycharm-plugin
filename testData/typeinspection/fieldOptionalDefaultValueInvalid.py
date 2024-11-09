
from typing import Optional

from pydantic import BaseModel


class A(BaseModel):
    a = '123'


A(<warning descr="Expected type 'str', got 'int' instead">a=int(123)</warning>)
