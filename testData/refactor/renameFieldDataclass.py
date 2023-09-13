from pydantic.dataclasses import dataclass

@dataclass
class Base:
    pass

@dataclass
class A(Base):
    ab<caret>c: str

@dataclass
class B(A):
    abc: int

@dataclass
class C(Base):
    abc: str

A(abc='abc')
B(abc='abc')
C(abc='abc')