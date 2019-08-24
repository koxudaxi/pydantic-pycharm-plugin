from pydantic import BaseModel, validator


class A(BaseModel):
    abc: str

class B(A):
    abc: str

class C(A):
    abc: str  # expected

A(abc='cde')
B(abc='cde')
C(ab<caret>c='cde')
## count: 1