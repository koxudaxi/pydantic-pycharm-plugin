from pydantic import BaseModel

class Base(BaseModel):
    pass

class A(Base):
    ab<caret>c: str
    xyz: str

class B(A):
    abc: int
    xyz: str

class C(Base):
    abc: str
    xyz: str

A(abc='abc', xyz='123')
B(abc='abc', xyz='123')
C(abc='abc', xyz='123')