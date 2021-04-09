

from pydantic import BaseModel, Field


class A(BaseModel):
    a: int = Field(int(123))
    b = Field(123)
    c = Field(default=int(123))
    d: int = Field(...)

A(a=int(123), b=int(123), c=int(123), d=int(123))
