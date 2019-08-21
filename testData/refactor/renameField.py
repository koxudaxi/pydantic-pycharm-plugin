from pydantic import BaseModel


class A(BaseModel):
    ab<caret>c: str

class B(A):
    abc: int

A(abc='abc')
B(abc='abc')