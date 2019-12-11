from pydantic import BaseModel


class A(BaseModel):
    <warning descr="Untyped fields disallowed">a = '123'</warning>

