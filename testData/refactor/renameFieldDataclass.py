from pydantic.dataclasses import dataclass

@dataclass
class A:
    ab<caret>c: str

class B(A):
    abc: int

A(abc='abc')
B(abc='abc')