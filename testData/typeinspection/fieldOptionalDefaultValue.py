
from typing import Optional

from pydantic import BaseModel


class A(BaseModel):
    a: int = None


A(a=int(123))
