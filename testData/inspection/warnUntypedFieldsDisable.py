from pydantic import BaseModel


class A(BaseModel):
    a = '123'


class B(BaseModel):
    b: str = '123'


class C:
    c = '123'


class D:
    d

def e():
    ee = '123'

f = '123'

class G(BaseModel):
    _g = '123'
    __g = '123'