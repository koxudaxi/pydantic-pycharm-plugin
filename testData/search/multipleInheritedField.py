from pydantic import BaseModel, validator


class A(BaseModel):
    ab<caret>c: str # expected

class B(A):
    abc: str # expected

class C(A):
    abc: str # expected

class D(B, C):
    abc: str # expected

D(abc='cde') # expected
## count: 5