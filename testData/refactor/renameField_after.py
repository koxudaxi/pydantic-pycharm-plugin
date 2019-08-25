from pydantic import BaseModel


class A(BaseModel):
    cde: str

class B(A):
    cde: int

A(cde='abc')
B(cde='abc')