from pydantic import BaseModel


class A(BaseModel):
    abc: str = '123'
A.abc = '456'
A().abc = '456'

class B(BaseModel):
    abc: str = '123'
    class Config:
        allow_mutation=False
B.abc = '456'
<error descr="Property \"abc\" defined in \"B\" is read-only">B().abc = '456'</error>

class C(BaseModel):
    abc: str = '123'
    class Config:
        allow_mutation=True
C.abc = '456'
C().abc = '456'

class D(BaseModel):
    class Config:
        allow_mutation=False
D.abc = '456'
<error descr="Property \"abc\" defined in \"D\" is read-only">D().abc = '456'</error>

allow_mutation = True
class E(BaseModel):
    class Config:
        allow_mutation=allow_mutation
E.abc = '456'
E().abc = '456'


class F(D):
    class Config:
        allow_mutation=False
F.abc = '456'
<error descr="Property \"abc\" defined in \"F\" is read-only">F().abc = '456'</error>


class G(BaseModel):
    class Config:
        allow_mutation=False
G.abc =<EOLError descr="Expression expected"></EOLError>
G.abc.lower()
<error descr="Can't assign to function call">G.abc.lower()</error> = 'efg'


class H:
    class Config:
        allow_mutation=False
H.abc = '123'