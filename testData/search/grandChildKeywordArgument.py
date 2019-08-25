from pydantic import BaseModel, validator


class A(BaseModel):
    abc: str # expected

class B(A):
    pass

class C(B):
    pass

A(abc='cde')
B(abc='cde')
C(ab<caret>c='cde')
## count: 1