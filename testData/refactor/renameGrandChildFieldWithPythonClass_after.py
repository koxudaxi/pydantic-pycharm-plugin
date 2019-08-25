from pydantic import BaseModel, validator


class A(BaseModel):
    cde: str

class B(A):
    pass

class D:
    pass

class C(B, D):
    cde: str



A(cde='cde')
B(cde='cde')
C(cde='cde')
## count: 0