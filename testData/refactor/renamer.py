from pydantic import BaseModel


class A(BaseModel):
    abc: str


A(ab<caret>c='abc')
