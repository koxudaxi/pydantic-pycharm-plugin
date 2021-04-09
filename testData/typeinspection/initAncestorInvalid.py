

from pydantic import BaseModel


class A(BaseModel):
    a: int


class B(A):
    def __init__(self, a):
        super().__init__(a=a)


B(<warning descr="Expected type 'int', got 'str' instead">a=str('123')</warning>)

