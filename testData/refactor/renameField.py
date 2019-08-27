from pydantic import BaseModel


class A(BaseModel):
    ab<caret>c: str
    xyz: str

class B(A):
    abc: int
    xyz: str

A(abc='abc', xyz='123')
B(abc='abc', xyz='123')