from pydantic import BaseModel, validator


class A(BaseModel):
    abc: str  # expected
    cde: str

class B(A):
    abc: str
    cde: str

A(ab<caret>c='cde')
B(abc='cde')
# count 1