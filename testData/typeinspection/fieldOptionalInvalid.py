
from typing import Optional

from pydantic import BaseModel


class A(BaseModel):
    a: Optional[str]


A(<warning descr="Expected type 'Optional[str]', got 'int' instead">a=int(123)</warning>)
