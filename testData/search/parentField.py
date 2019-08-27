from pydantic import BaseModel, validator


class A(BaseModel):
    ab<caret>c: str # expected
    cde: str

class B(A):
    abc: str # expected
    cde: str

A(abc='cde') # expected
B(abc='cde') # expected
## count: 4