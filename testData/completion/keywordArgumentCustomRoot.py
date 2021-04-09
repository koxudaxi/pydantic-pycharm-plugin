

from pydantic import BaseModel


class A(BaseModel):
    __root__: str

class B(A):
    hij: str

A(<caret>)