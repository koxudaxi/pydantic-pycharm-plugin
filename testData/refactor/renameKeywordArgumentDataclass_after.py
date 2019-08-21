from pydantic.dataclasses import dataclass

@dataclass
class A:
    cde: str

class B(A):
    cde: int

A(cde='abc')
B(cde='abc')
