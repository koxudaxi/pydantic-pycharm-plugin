from pydantic import BaseModel, validator


class A(BaseModel):
    a: str

    @validator('a')
    def vali<caret>date_a(cls):
        pass
