from builtins import *
import typing

from pydantic import BaseModel


class A(BaseModel):
    a: typing.Union[int, str, None]
    b: typing.Optional[str]
    c: typing.Annotated[str, Field(example='abc')]
    d: typing.Any

A(<warning descr="Expected type 'Union[int, str, None]', got 'bytes' instead">a=bytes(123)</warning>, <warning descr="Expected type 'Optional[str]', got 'bytes' instead">b=bytes(456)</warning>, <warning descr="Expected type 'str', got 'bytes' instead">c=bytes(789)</warning>, d=bytes(987))
A(<warning descr="Expected type 'Union[int, str, None]', got 'bytes' instead">a=bytes(123)</warning>, <warning descr="Expected type 'Optional[str]', got 'bytes' instead">b=bytes(456)</warning>, <warning descr="Expected type 'str', got 'bytes' instead">c=bytes(789)</warning>, d=bytes(987))
A(<warning descr="Expected type 'Union[int, str, None]', got 'bytes' instead">a=bytes(123)</warning>, <warning descr="Expected type 'Optional[str]', got 'bytes' instead">b=bytes(456)</warning>, <warning descr="Expected type 'str', got 'bytes' instead">c=bytes(789)</warning>, d=bytes(987))