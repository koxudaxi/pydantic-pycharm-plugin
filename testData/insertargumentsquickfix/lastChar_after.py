from typing import Optional
from pydantic import BaseModel, Field


class A(BaseModel):
    a: int
    b: int = ...
    c: int = 123
    d: int = Field(123)
    e: int = Field(...)
    f: Optional[int]
    g: int = Field(default=123)
    h: int = Field(default=...)
    i: int = Field(default_factory=lambda: 123)
    j: int = Field(default_factory=int)


A(a=, b=, c=123, d=123, e=, f=, g=123, h=, i=lambda: 123, j=int)
