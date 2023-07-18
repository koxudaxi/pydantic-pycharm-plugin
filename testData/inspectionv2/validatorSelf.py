from pydantic import BaseModel, field_validator, model_validator

def check(func):
    def inner():
        func()
    return inner

class A(BaseModel):
    a: str
    b: str
    c: str
    d: str
    e: str

    @field_validator('a')
    def validate_a(<weak_warning descr="Usually first parameter of such methods is named 'cls'">self</weak_warning>):
        pass

    @field_validator('b')
    def validate_b(<weak_warning descr="Usually first parameter of such methods is named 'cls'">fles</weak_warning>):
        pass

    @field_validator('c')
    def validate_b(*args):
        pass

    @field_validator('d')
    def validate_c(**kwargs):
        pass

    @field_validator('e')
    def validate_e<error descr="Method must have a first parameter, usually called 'cls'">()</error>:
        pass

    @model_validator()
    def validate_model<error descr="Method must have a first parameter, usually called 'cls'">()</error>:
        pass


    def dummy(self):
        pass

    @check
    def task(self):
        pass