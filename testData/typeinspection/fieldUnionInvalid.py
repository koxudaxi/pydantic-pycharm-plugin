from typing import Union

from pydantic import BaseModel


class A(BaseModel):
    a: Union[float, int]


A(<warning descr="Expected type 'Union[float, int]', got 'bytes' instead">a=bytes(123)</warning>)
A(<warning descr="Expected type 'Union[float, int]', got 'str' instead">a=str('123')</warning>)
