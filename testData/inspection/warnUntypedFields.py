from pydantic import BaseModel


class A(BaseModel):
    <warning descr="Untyped fields disallowed">a = '123'</warning>


class B(BaseModel):
    b: str = '123'


class C:
    c = '123'

class D:
    d