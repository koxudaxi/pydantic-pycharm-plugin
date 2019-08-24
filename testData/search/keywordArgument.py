from pydantic import BaseModel, validator


class A(BaseModel):
    abc: str # expected

class B(A):
    abc: str


A(ab<caret>c='cde')
B(abc='cde')
# count 1