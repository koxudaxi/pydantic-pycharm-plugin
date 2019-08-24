from builtins import *

from pydantic import BaseModel


class A(BaseModel):
    a: int

    @classmethod
    def test(cls):
        return cls(int(123))


