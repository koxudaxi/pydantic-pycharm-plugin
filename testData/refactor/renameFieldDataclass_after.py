from pydantic.dataclasses import dataclass

@dataclass
class Base:
    pass

@dataclass
class A(Base):
    cde: str

@dataclass
class B(A):
    cde: int

@dataclass
class C(Base):
    abc: str

A(cde='abc')
B(cde='abc')
C(abc='abc')