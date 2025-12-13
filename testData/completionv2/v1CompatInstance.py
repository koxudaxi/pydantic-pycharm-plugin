

from pydantic.v1 import BaseModel


class A(BaseModel):
    abc: str
    cde: str = 'abc'
    efg: str = 'abc'


a = A(abc='a')
a.<caret>
