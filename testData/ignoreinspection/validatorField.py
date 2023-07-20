from pydantic import BaseModel, validator


class A(BaseModel):
    a: str

    @validator('x<caret>')
    def validate_a(cls):
        pass
