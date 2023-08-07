from typing import ClassVar
from pydantic import BaseModel, RootModel


class <error descr="__root__ models are no longer supported in v2; a migration guide will be added in the near future">A</error>(BaseModel):
    <warning descr="To define root models, use `pydantic.RootModel` rather than a field called '__root__'">__root__</warning> = 'xyz'


class B(BaseModel):
    a = 'xyz'

class C:
    __root__ = 'xyz'
    e = 'xyz'

def d():
    __root__ = 'xyz'
    g = 'xyz'


class A(RootModel):
    root = 'xyz'


class B(RootModel):
    <warning descr="Unexpected field with name a; only 'root' is allowed as a field of a `RootModel`">a</warning> = 'xyz'


class C(RootModel):
    root = 'xyz'
    <warning descr="Unexpected field with name b; only 'root' is allowed as a field of a `RootModel`">b</warning> = 'xyz'


class D(RootModel):
    root = 'xyz'
    _c = 'xyz'
    __c = 'xyz'

class E:
    root = 'xyz'
    e = 'xyz'

def f():
    root = 'xyz'
    g = 'xyz'

class G(RootModel):
    ATTRIBUTE_NAME: ClassVar[str] = "testing"
    root = 'xyz'

class H(RootModel):
    root = 'xyz'
    <warning descr="Unexpected field with name b; only 'root' is allowed as a field of a `RootModel`">b</warning>: str

