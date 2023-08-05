from pydantic import BaseModel, field_validator, model_validator

def check(func):
    def inner():
        func()
    return inner

FALSE = False

TRUE = True

def get_check_fields():
    pass
class A(BaseModel):
    a: str
    b: str
    c: str
    d: str
    e: str

    @field_validator('a')
    def validate_a(cls):
        pass

    @field_validator(<error descr="Cannot find field 'x'">'x'</error>)
    def validate_b(cls):
        pass

    @field_validator(<error descr="Cannot find field 'x'">'x'</error>, check_fields=True)
    def validate_a(cls):
        pass
    @field_validator('x', check_fields=False)
    def validate_a(cls):
        pass

    @field_validator('x', check_fields=FALSE)
    def validate_a(cls):
        pass

    @field_validator(<error descr="Cannot find field 'x'">'x'</error>, check_fields=TRUE)
    def validate_a(cls):
        pass
    @field_validator('x', check_fields=get_check_fields())
    def validate_a(cls):
        pass
    @field_validator('c')
    def validate_b(*args):
        pass

    @field_validator('d')
    def validate_c(**kwargs):
        pass

    @field_validator('*')
    def validate_c(**kwargs):
        pass

    @model_validator(model='before')
    def validate_model(cls):
        pass

    def dummy(self):
        pass

    @check
    def task(self):
        pass