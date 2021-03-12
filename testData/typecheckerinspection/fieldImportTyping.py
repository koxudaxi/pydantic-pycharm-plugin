from builtins import *
import typing

from pydantic import BaseModel


class A(BaseModel):
    a: typing.Union[int, str, None]
    b: typing.Optional[str]
    c: typing.Annotated[str, Field(example='abc')]
    d: typing.Any
    e: typing.Annotated[typing.Optional[str], Field(example='abc')]

A(a=int(123), b=None, c='xyz', d='456', e='789')
A(a=str('123'), b='efg', c='xyz', d='456', e=None)
A(a=None, b=str('efg'), c=str('xyz'), d='456')
