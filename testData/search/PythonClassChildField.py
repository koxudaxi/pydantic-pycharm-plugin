from pydantic import BaseModel, validator


class A:
    abc: str # expected

class B(A):
    ab<caret>c: str # expected

A(abc='cde')
B(abc='cde')
## count: 2