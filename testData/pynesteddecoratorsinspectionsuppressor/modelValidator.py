from pydantic import BaseModel, model_validator

class Foo(BaseModel):
    value: int

    @model_validator(mode="before")
    @classmethod
    def whatever(cls, data):
        return data
