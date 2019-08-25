from pydantic import BaseModel


class A:
    abc: str


A(ab<caret>c='abc')
