

from pydantic import BaseModel


class A(BaseModel):
    a: str


A(<warning descr="Expected type 'str', got 'int' instead">a=int(123)</warning>)
