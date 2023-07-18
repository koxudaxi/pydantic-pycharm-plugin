
from pydantic import BaseModel, field_validator


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


    @field_validator('<caret>')
    def validate(self, values):
        return values