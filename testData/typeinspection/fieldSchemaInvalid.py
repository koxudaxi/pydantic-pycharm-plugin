

from pydantic import BaseModel, Schema


class A(BaseModel):
    a: str = Schema('123')
    b = Schema('123')
    c = Schema(default='123')

A(<warning descr="Expected type 'str', got 'int' instead">a=int('123')</warning>, <warning descr="Expected type 'str', got 'int' instead">b=int('123')</warning>, <warning descr="Expected type 'str', got 'int' instead">c=int('123')</warning>)
