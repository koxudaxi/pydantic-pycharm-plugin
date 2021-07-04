from pydantic import BaseModel, Extra

class A(BaseModel):
    class Config:
        extra = Extra.ignore

A(a='123')


class B(BaseModel):
    a: str
    class Config:
        extra = Extra.forbid

B(a='abc', <error descr="'b' extra fields not permitted">b='123'</error>)