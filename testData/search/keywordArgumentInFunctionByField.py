from pydantic import BaseModel, validator
from typing import Type

class A(BaseModel):
    abc: str # expected

def func(instance: A):
    instance(ab<caret>c)
## count: 2