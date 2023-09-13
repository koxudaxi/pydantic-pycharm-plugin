from pydantic import BaseModel


class Base(BaseModel):
    pass

class A(Base):
    cde: str

class B(A):
    cde: int

class C(Base):
    abc: int

A(cde='abc')
B(cde='abc')
C(abc='abc')
