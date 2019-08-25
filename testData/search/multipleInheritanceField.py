from pydantic import BaseModel, validator


class A(BaseModel):
    abc: str # expected

class B(A):
    pass

class C(A):
    pass

class D(B, C):
    ab<caret>c: str # expected

D(abc='cde') # expected
## count: 3