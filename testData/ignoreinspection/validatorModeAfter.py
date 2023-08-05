from pydantic import BaseModel, model_validator


class A(BaseModel):
    a: str

    @model_validator(mode='after')
    def vali<caret>date_a(self):
        pass
