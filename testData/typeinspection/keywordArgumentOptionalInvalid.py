from builtins import *
from typing import Optional

from pydantic import BaseModel


class A(BaseModel):
    a: Optional[str]


A(a=str(123))

