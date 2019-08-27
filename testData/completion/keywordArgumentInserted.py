from pydantic import BaseModel


class A(BaseModel):
    abc: str
    cde: str
    efg: str


A(abc='abc',<caret>)