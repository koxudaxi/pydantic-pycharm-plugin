from pydantic import BaseModel, validator


class A(BaseModel):
    abc: str

class B(A):
    abc: str # expected


A(abc='cde')
B(ab<caret>c='cde')
# count 1