from builtins import *

from pydantic import BaseModel


class A(BaseModel):
    a: int

    def __init__(self, a):
        super().__init__(a=a)


A(<warning descr="Expected type 'int', got 'str' instead">a=str('123')</warning>)

