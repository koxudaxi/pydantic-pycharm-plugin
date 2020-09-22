from pydantic import BaseModel
from pydantic.class_validators import validator


class A(BaseModel):
    a: str

    @validator('a')
    def vali<caret>date_a(cls):
        pass
