
from typing import Annotated
from pydantic import BaseModel, Field
from pydantic.dataclasses import dataclass

class A(BaseModel):
    abc: Annotated[str, Field(example='example')]
    abcefg: Annotated[str, Field(default='example')]
    abcefghij: Annotated[str, Field('example')]
    cde: Annotated[str, Field(default='default')] = <warning descr="`Field` default cannot be set in `Annotated` for 'cde'">'default_value'</warning>
    efg: Annotated[str, Field(default_factory=lambda: 123)]
    hij: Annotated[str, Field(default='default', <warning descr="cannot specify both default and default_factory">default_factory=lambda: 123</warning>)]
    klm: Annotated[str, Field(default_factory=lambda: 123)] = <warning descr="cannot specify `Annotated` and value `Field`s together for 'klm'">Field(default='default')</warning>
    nop: int = Field(default='default', <warning descr="cannot specify both default and default_factory">default_factory=lambda: 123</warning>)
    qrs: int = Field('default', <warning descr="cannot specify both default and default_factory">default_factory=lambda: 123</warning>)
    tuv: Annotated[int, Field('default', <warning descr="cannot specify both default and default_factory">default_factory=lambda: 123</warning>)]
    wxy: Annotated[int, Field(<warning descr="cannot specify both default and default_factory">default_factory=lambda: 123</warning>)] = 'default'
    a12: Annotated[str, Field(<warning descr="cannot specify both default and default_factory">default_factory=lambda: 123</warning>, default='default')]
    a34: Annotated[str, Field(example='example')] = 'default'

@dataclass
class B:
    abc: Annotated[str, Field(example='example')]
    abcefg: Annotated[str, Field(default='example')]
    abcefghij: Annotated[str, Field('example')]
    cde: Annotated[str, Field(default='default')] = <warning descr="`Field` default cannot be set in `Annotated` for 'cde'">'default_value'</warning>
    efg: Annotated[str, Field(default_factory=lambda: 123)]
    hij: Annotated[str, Field(default='default', <warning descr="cannot specify both default and default_factory">default_factory=lambda: 123</warning>)]
    klm: Annotated[str, Field(default_factory=lambda: 123)] = <warning descr="cannot specify `Annotated` and value `Field`s together for 'klm'">Field(default='default')</warning>
    nop: int = Field(default='default', <warning descr="cannot specify both default and default_factory">default_factory=lambda: 123</warning>)
    qrs: int = Field('default', <warning descr="cannot specify both default and default_factory">default_factory=lambda: 123</warning>)
    tuv: Annotated[int, Field('default', <warning descr="cannot specify both default and default_factory">default_factory=lambda: 123</warning>)]
    wxy: Annotated[int, Field(<warning descr="cannot specify both default and default_factory">default_factory=lambda: 123</warning>)] = 'default'
    a12: Annotated[str, Field(<warning descr="cannot specify both default and default_factory">default_factory=lambda: 123</warning>, default='default')]
    a34: Annotated[str, Field(example='example')] = 'default'