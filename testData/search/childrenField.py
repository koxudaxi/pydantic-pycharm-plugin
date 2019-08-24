from pydantic import BaseModel, validator


class A(BaseModel):
    abc: str # expected

class B(A):
    abc: str

class C(B):
    ab<caret>c: str # expected

A(abc='cde')
B(abc='cde')
C(abc='cde') # expected
## count: 3