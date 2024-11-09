

from pydantic import BaseModel


class A(BaseModel):
    _abc: str = 'abc'
    __cde: str = 'abc'


class B(A):
    _efg: str = 'abc'
    __hij: str = 1

B(<caret>)