from pydantic import BaseModel, validator


class A:
    abc: str

class B(A):
    abc: str


A(abc='cde')
B(ab<caret>c='cde')
# count 0