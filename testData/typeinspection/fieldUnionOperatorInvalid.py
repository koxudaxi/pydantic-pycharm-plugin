from pydantic import BaseModel


class A(BaseModel):
    a: str | bytes


A(<warning descr="Expected type 'str | bytes', got 'int' instead">a=int(123)</warning>)
