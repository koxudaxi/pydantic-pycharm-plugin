from pydantic.dataclasses import dataclass

@dataclass
class A:
    abc: str

@dataclass
class B(A):
    abc: int

A(ab<caret>c='abc')
B(abc='abc')
