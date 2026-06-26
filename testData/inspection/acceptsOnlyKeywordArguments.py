from pydantic import BaseModel
from pydantic.dataclasses import dataclass

class A(BaseModel):
    a: str


A(<warning descr="class 'A' accepts only keyword arguments">'a'</warning><warning descr="null">)</warning>

@dataclass
class B():
    a: str


B('a')

class C(BaseModel):
    a: str
    def __call__(self, *args, **kwargs):
        pass

c = C(a='abc')

c('a')

@dataclass
class D():
    a: str
    b: str


D('a'<warning descr="null">)</warning>


class E(BaseModel):
    pass

E()
