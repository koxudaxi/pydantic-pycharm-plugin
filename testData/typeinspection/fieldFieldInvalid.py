

from pydantic import BaseModel, Field


class A(BaseModel):
    a: int = Field(int(123))
    b = Field(int(123))
    c = Field(default=int(123))

A(<warning descr="Expected type 'int', got 'str' instead">a=str('123')</warning>, <warning descr="Expected type 'int', got 'str' instead">b=str('123')</warning>, <warning descr="Expected type 'int', got 'str' instead">c=str('123')</warning>)
