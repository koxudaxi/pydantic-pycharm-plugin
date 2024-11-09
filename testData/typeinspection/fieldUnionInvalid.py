from typing import Union

from pydantic import BaseModel


class A(BaseModel):
    a: Union[str, bytes]


A(<warning descr="Expected type 'Union[str, bytes]', got 'int' instead">a=int(123)</warning>)
