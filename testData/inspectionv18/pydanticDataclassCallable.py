from pydantic.dataclasses import dataclass


@dataclass
class A:
    a: str
    b: int


A(a="x", b=1)
