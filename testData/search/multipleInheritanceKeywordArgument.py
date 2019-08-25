from pydantic import BaseModel, validator


class A(BaseModel):
    abc: str # expected

class B(A):
    pass

class C(A):
    pass

class D:
    abc: str

class F(B, C, D):
    pass

F(a<caret>bc='cde')
## count: 1