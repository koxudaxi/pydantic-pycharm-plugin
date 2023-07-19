
from pydantic import BaseModel, validator


class A(BaseModel):
    abc: str
    cde: str
    efg: str

class B(A):
    cde: str
    efg: str
    hij: str

class C(B):
    efg: str
    klm: str


    @validator('<caret>')
    def validate(self, values):
        return values