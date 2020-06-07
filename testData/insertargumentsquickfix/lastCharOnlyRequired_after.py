from pydantic import BaseModel, Field


class A(BaseModel):
    a: int
    b: int = ...
    c: int = 123
    d: int = Field(123)
    e: int = Field(...)


A(a=, b=, e=)

