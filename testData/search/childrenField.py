from pydantic import BaseModel, validator


class A(BaseModel):
    abc: str # expected

class B(A):
    abc: str  # expected

class C(B):
    ab<caret>c: str

A(abc='cde')
B(abc='cde')
C(abc='cde') # expected
## count: 3