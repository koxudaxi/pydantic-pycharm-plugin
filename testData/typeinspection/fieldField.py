

from pydantic import BaseModel, Field

NUMBER = 123

class A(BaseModel):
    a: int = Field(int(123))
    b = Field(123)
    c = Field(default=int(123))
    d: int = Field(...)
    e: int = Field(NUMBER)
    f: int = Field(default=NUMBER)
    g = Field(NUMBER)
    h: int = Field(NUMBER)
    i: int = Field(default=NUMBER)
    j = Field(NUMBER)

A(a=int(123), b=int(123), c=int(123), d=int(123), e=int(123), f=int(123), g=int(123))
