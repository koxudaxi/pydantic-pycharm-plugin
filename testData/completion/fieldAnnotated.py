
from typing import Annotated
from pydantic import BaseModel, Field

class Info:
    pass
class A(BaseModel):
    abc: Annotated[str, Field(example='example')]
    cde: Annotated[str, Field(example='example')] = 'default_value'
    efg: Annotated[str, Field(default_factory=lambda: 123)]
    a_id: Annotated[str, Field(alias='alias_a_id')]
    klm: Annotated[str, Info(), Field(default_factory=lambda: 456)]
    nop: Annotated[str, Field(default_factory=lambda: 789), Info()]
class B(A):
    hij: str

A().<caret>