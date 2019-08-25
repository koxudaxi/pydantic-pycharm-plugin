from pydantic import BaseModel


class A(BaseModel):
    abc: str

class B(A):
    abc: int

A(ab<caret>c='abc')
B(abc='abc')
