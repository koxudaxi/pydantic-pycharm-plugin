from typing import ClassVar

from pydantic import BaseModel


class A(BaseModel):
    __root__ = 'xyz'


class B(BaseModel):
    a = 'xyz'


class C(BaseModel):
    __root__ = 'xyz'
    <warning descr="__root__ cannot be mixed with other fields">b</warning> = 'xyz'


class D(BaseModel):
    __root__ = 'xyz'
    _c = 'xyz'
    __c = 'xyz'

class E:
    __root__ = 'xyz'
    e = 'xyz'

def f():
    __root__ = 'xyz'
    g = 'xyz'

class G(BaseModel):
    ATTRIBUTE_NAME: ClassVar[str] = "testing"
    __root__ = 'xyz'

class H(BaseModel):
    __root__ = 'xyz'
    <warning descr="__root__ cannot be mixed with other fields">b</warning>: str

