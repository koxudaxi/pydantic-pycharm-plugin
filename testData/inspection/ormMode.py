from pydantic import BaseModel


class A(BaseModel):
    pass
<error descr="You must have the config attribute orm_mode=True to use from_orm">A.from_orm('')</error>

class B(BaseModel):
    class Config:
        orm_mode=False
<error descr="You must have the config attribute orm_mode=True to use from_orm">B.from_orm('')</error>


class C(BaseModel):
    class Config:
        orm_mode=True
C.from_orm('')

class C(BaseModel):
    class Config:
        orm_mode=False
<error descr="You must have the config attribute orm_mode=True to use from_orm">C.from_orm('')</error>

orm_mode = True
class D(BaseModel):
    class Config:
        orm_mode=orm_mode
D.from_orm('')


class E(D):
    class Config:
        orm_mode=False
<error descr="You must have the config attribute orm_mode=True to use from_orm">E.from_orm('')</error>

class A(BaseModel):
    def __call__(self, *args, **kwargs):
        class Inner:
            @classmethod
            def from_orm(self, *args, **kwargs):
                pass
        return Inner

A()().from_orm('')
