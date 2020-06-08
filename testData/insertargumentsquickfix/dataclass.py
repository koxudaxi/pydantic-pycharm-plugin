from typing import Optional
from pydantic import BaseModel, Field
from pydantic.dataclasses import dataclass

@dataclass
class A:
    a: int
    b: int = ...
    c: int = 123
    d: int = Field(123)
    e: int = Field(...)
    f: Optional[int]


A<caret>()
