from pydantic import BaseModel, validator

def deco(func):
    pass
class A(BaseModel):
    a: str

    @deco('x<caret>')
    def validate_a(cls):
        pass
