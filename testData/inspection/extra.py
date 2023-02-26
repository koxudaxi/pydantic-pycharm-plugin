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
params = {'a': 'abc', 'b': '123'}
B(**params)


class C(BaseModel):
    id: int


class D(C):
    data: List[int]

    class Config:
        extra = Extra.forbid


d = D(id=1, data=[1, 2, 3])

class E(BaseModel):
    a: str
    class Config:
        extra = Extra.forbid

    def __call__(self, *args, **kwargs):
        pass

e = E(a='abc')

e(a='efg', b='xyz')
