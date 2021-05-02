from pydantic import BaseModel


class B(BaseModel, allow_mutation=False):
    abc: str = '123'
B.abc = '456'

class C(BaseModel, allow_mutation=True):
    abc: str = '123'
C.abc = '456'
C().abc = '456'

class D(BaseModel, allow_mutation=False):
    pass
D.abc = '456'

allow_mutation = True
class E(BaseModel, allow_mutation=allow_mutation):
    pass
E.abc = '456'
E().abc = '456'


class F(D, allow_mutation=False):
    pass
F.abc = '456'
