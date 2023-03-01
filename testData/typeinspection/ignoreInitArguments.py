

from pydantic import BaseModel


class A(BaseModel):
    a: int

    def __init__(self, xyz: str):
        super().__init__(a=xyz)

A(xyz=123<warning descr="null">)</warning>

A(a=123)