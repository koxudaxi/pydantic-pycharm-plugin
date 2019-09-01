from builtins import *

from pydantic import BaseModel


class A(BaseModel):
    abc: str

    @classmethod
    def test(cls):
        return cls.<caret>


