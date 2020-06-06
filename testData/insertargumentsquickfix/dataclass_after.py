from pydantic import BaseModel, Field
from pydantic.dataclasses import dataclass

@dataclass
class A:
    a: int
    b: int = ...
    c: int = 123
    d: int = Field(123)
    e: int = Field(...)


A(a=, b=, c=123, d=123, e=)
