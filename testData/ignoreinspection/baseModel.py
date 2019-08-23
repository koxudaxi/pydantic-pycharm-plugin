from pydantic import BaseModel, validator


class A(BaseModel):
    a: str

    def vali<caret>date_a(cls):
        pass
