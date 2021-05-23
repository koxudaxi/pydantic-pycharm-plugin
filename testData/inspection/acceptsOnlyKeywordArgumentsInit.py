from pydantic import BaseModel
from pydantic.dataclasses import dataclass

class A(BaseModel):
    a: str

    def __init__(self, a = '123') -> None:
        super(A, self).__init__(a=a)

A('a')

@dataclass
class B():
    a: str


B('a')
