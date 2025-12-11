from pydantic import BaseModel


class Foo(BaseModel, frozen=True):
    a: str


class Bar(BaseModel):
    b: int


class ChildFrozen(Bar, frozen=True):
    c: str


Foo(a="hello")
Bar(b=1)
ChildFrozen(b=2, c="world")
