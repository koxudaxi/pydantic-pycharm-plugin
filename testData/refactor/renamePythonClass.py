from pydantic import BaseModel


class A:
    ab<caret>c: str


A(abc='abc')
