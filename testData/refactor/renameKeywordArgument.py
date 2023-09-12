from pydantic import BaseModel


class Base(BaseModel):
    pass

class A(Base):
    abc: str

class B(A):
    abc: int

class C(Base):
    abc: int

A(ab<caret>c='abc')
B(abc='abc')
C(abc='abc')
