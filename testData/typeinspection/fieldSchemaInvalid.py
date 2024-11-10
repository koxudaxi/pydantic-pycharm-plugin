

from pydantic import BaseModel, Schema


class A(BaseModel):
    a: int = Schema(int(123))
    b = Schema(int(123))
    c = Schema(default=int(123))

A(<warning descr="Expected type 'int', got 'str' instead">a=str('123')</warning>, <warning descr="Expected type 'int', got 'str' instead">b=str('123')</warning>, <warning descr="Expected type 'int', got 'str' instead">c=str('123')</warning>)
