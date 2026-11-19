from pydantic import BaseModel


class A(BaseModel):
    value: str


A(<warning descr="Expected type 'str', got 'bytes' instead">value=bytes(123)</warning>)
A(<weak_warning descr="Field is of type 'str', 'int' is set as an acceptable type in pyproject.toml">value=int(123)</weak_warning>)
