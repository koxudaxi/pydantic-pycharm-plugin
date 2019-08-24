from builtins import *

from pydantic import BaseModel


class A(BaseModel):
    a: int

    @classmethod
    def test(cls):
        return cls(<warning descr="Expected type 'int', got 'str' instead">str('123')</warning>)
