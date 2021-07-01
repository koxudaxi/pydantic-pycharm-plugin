

from pydantic import BaseModel, Field

NUMBER = 123

class A(BaseModel):
    a: int = Field(int(123))
    b = Field(int(123))
    c = Field(default=int(123))
    e: int = Field(NUMBER)
    f: int = Field(default=NUMBER)
    g = Field(NUMBER)

A(<warning descr="Expected type 'int', got 'str' instead">a=str('123')</warning>, <warning descr="Expected type 'int', got 'str' instead">b=str('123')</warning>, <warning descr="Expected type 'int', got 'str' instead">c=str('123')</warning>, <warning descr="Expected type 'int', got 'str' instead">e=str('123')</warning>, <warning descr="Expected type 'int', got 'str' instead">f=str('123')</warning>, <warning descr="Expected type 'int', got 'str' instead">g=str('123')</warning>)
