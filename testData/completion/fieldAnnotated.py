from builtins import *
from typing import Annotated
from pydantic import BaseModel, Field


class A(BaseModel):
    abc: Annotated[str, Field(example='example')]
    cde: Annotated[str, Field(example='example')] = 'default_value'
    efg: Annotated[str, Field(default_factory=lambda: 123)]
    a_id: Annotated[str, Field(alias='alias_a_id')]
class B(A):
    hij: str

A().<caret>