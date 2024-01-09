
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
    default_abc: Annotated[str, Field('example')]
    default_efg: Annotated[str, Field(default='123')]
    default_klm: Annotated[str, Info(), Field(default='456')]
    default_nop: Annotated[str, Field(default='789'), Info()]
    default_klm_positional: Annotated[str, Info(), Field('456')]
    default_nop_positional: Annotated[str, Field('789'), Info()]
class B(A):
    hij: str

A().<caret>