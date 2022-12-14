from typing import Optional
from dataclasses import field
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
    g: int = Field(default=123)
    h: int = Field(default=...)
    i: int = Field(default_factory=lambda: 123)
    j: int = Field(default_factory=int)
    k: int = field(123)
    l: int = field(...)
    m: int = field(default=123)
    n: int = field(default=...)
    o: int = field(default_factory=lambda: 123)
    p: int = field(default_factory=int)


A(a=, b=, c=123, d=123, e=, f=, g=123, h=, i=lambda: 123, j=int, k=123, l=, m=123, n=, o=lambda: 123, p=int)
