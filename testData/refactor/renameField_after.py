from pydantic import BaseModel


class A(BaseModel):
    cde: str
    xyz: str

class B(A):
    cde: int
    xyz: str

A(cde='abc', xyz='123')
B(cde='abc', xyz='123')