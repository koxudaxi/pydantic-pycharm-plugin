from pydantic import BaseModel, validator


class A(BaseModel):
    abc: str # expected

class B(A):
    ab<caret>c: str # expected

A(abc='cde')
B(abc='cde') # expected
## count: 3