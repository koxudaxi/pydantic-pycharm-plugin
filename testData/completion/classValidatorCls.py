,from builtins import *

from pydantic import BaseModel, validator


class A(BaseModel):
    abc: str

    @validator('abc')
    def test(cls):
        return cls.<caret>


