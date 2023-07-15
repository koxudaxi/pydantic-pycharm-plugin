from pydantic import BaseModel, ConfigDict


class A(BaseModel):
    abc: str = '123'
A.abc = '456'
A().abc = '456'

class B(BaseModel):
    abc: str = '123'
    model_config = ConfigDict(frozen=True)

B.abc = '456'
<error descr="Property \"abc\" defined in \"B\" is read-only">B().abc = '456'</error>

class C(BaseModel):
    abc: str = '123'
    model_config = ConfigDict(frozen=False)
C.abc = '456'
C().abc = '456'

class D(BaseModel):
    model_config = ConfigDict(frozen=True)
D.abc = '456'
<error descr="Property \"abc\" defined in \"D\" is read-only">D().abc = '456'</error>

frozen = False
class E(BaseModel):
    model_config = ConfigDict(frozen=frozen)
E.abc = '456'
E().abc = '456'


class F(D):
    model_config = ConfigDict(frozen=True)
F.abc = '456'
<error descr="Property \"abc\" defined in \"F\" is read-only">F().abc = '456'</error>


class G(BaseModel):
    model_config = ConfigDict(frozen=False)
G.abc =<EOLError descr="Expression expected"></EOLError>
G.abc.lower()
<error descr="Cannot assign to function call">G.abc.lower()</error> = 'efg'


class H:
    model_config = ConfigDict(frozen=True)
H.abc = '123'
