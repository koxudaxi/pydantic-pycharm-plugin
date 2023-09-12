from pydantic import BaseModel

class Base(BaseModel):
    pass

class A(Base):
    cde: str
    xyz: str

class B(A):
    cde: int
    xyz: str

class C(Base):
    abc: str
    xyz: str

A(cde='abc', xyz='123')
B(cde='abc', xyz='123')
C(abc='abc', xyz='123')