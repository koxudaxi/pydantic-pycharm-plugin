from pydantic import BaseModel


class A(BaseModel):
    abc: str = '123'
A.abc = '456'
A().abc = '456'

class B(BaseModel):
    abc: str = '123'
    class Config:
        frozen=True
B.abc = '456'
<error descr="Property \"abc\" defined in \"B\" is read-only">B().abc = '456'</error>

class C(BaseModel):
    abc: str = '123'
    class Config:
        frozen=False
C.abc = '456'
C().abc = '456'

class D(BaseModel):
    class Config:
        frozen=True
D.abc = '456'
<error descr="Property \"abc\" defined in \"D\" is read-only">D().abc = '456'</error>

frozen = False
class E(BaseModel):
    class Config:
        frozen=frozen
E.abc = '456'
E().abc = '456'


class F(D):
    class Config:
        frozen=True
F.abc = '456'
<error descr="Property \"abc\" defined in \"F\" is read-only">F().abc = '456'</error>


class G(BaseModel):
    class Config:
        frozen=False
G.abc =<EOLError descr="Expression expected"></EOLError>
G.abc.lower()
<error descr="Cannot assign to function call">G.abc.lower()</error> = 'efg'


class H:
    class Config:
        frozen=True
H.abc = '123'
