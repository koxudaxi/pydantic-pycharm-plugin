from builtins import *

from typing import *

from pydantic import BaseModel


class A(BaseModel):
    a: int
    b: Any
    c: Optional[int]
    d: Union[str, int, None]


<warning descr="Insert required arguments">A(a=int(123))</warning>
A(a=int(123), b=456, c=789, d=345)
A(<warning descr="Expected type 'int', got 'str' instead">a=str(123)</warning>, b=456, <warning descr="Expected type 'Optional[int]', got 'str' instead">c=str(789)</warning>, <warning descr="Expected type 'Union[str, int, None]', got 'bytes' instead">d=bytes(234)</warning>)