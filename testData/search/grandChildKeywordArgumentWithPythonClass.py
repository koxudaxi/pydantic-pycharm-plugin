from pydantic import BaseModel, validator


class A(BaseModel):
    pass

class B(A):
    pass

class D:
    pass

class C(B, D):
    pass



A(abc='cde')
B(abc='cde')
C(ab<caret>c='cde')
## count: 0