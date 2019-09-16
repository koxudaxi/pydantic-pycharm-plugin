from pydantic import BaseModel, validator


class A(BaseModel):
    ab<caret>c: str # expected

def func(instance: A):
    instance.abc
## count: 2