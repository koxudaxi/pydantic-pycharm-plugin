from builtins import *
from typing import Annotated
from pydantic import BaseModel, Field


class A(BaseModel):
    abc: Annotated[str, Field(example='example')]
    cde: Annotated[str, Field(default='default')] = <warning descr="`Field` default cannot be set in `Annotated` for 'cde'">'default_value'</warning>
    efg: Annotated[str, Field(default_factory=lambda: 123)]
    hij: Annotated[str, Field(<warning descr="`Field` default cannot be set in `Annotated` for 'hij'">default='default'</warning>, default_factory=lambda: 123)]
    klm: Annotated[str, Field(default_factory=lambda: 123)] = <warning descr="cannot specify `Annotated` and value `Field`s together for 'klm'">Field(default='default')</warning>
    nop: str = Field(default='default', <warning descr="cannot specify both default and default_factory">default_factory=lambda: 123</warning>)
    qrs: str = Field('default', <warning descr="cannot specify both default and default_factory">default_factory=lambda: 123</warning>)
    tuv: Annotated[str, Field<warning descr="`Field` default cannot be set in `Annotated` for 'tuv'">('default', default_factory=lambda: 123)</warning>]
    wxy: Annotated[str, Field(<warning descr="cannot specify both default and default_factory">default_factory=lambda: 123</warning>)] = 'default'
    a12: Annotated[str, Field(default_factory=lambda: 123, <warning descr="`Field` default cannot be set in `Annotated` for 'a12'">default='default'</warning>)]
    a34: Annotated[str, Field(example='example')] = 'default'