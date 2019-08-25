from pydantic import BaseModel, validator


class A(BaseModel):
    pass

class B(A):
    pass

class D:
    pass

class C(B, D):
    ab<caret>c: str



A(abc='cde')
B(abc='cde')
C(abc='cde')
## count: 0