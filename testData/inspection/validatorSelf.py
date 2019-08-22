from pydantic import BaseModel, validator

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

    @validator('a')
    def validate_a(<weak_warning descr="Usually first parameter of such methods is named 'cls'">self</weak_warning>):
        pass

    @validator('b')
    def validate_b(<weak_warning descr="Usually first parameter of such methods is named 'cls'">fles</weak_warning>):
        pass

    @validator('c')
    def validate_b(*args):
        pass

    @validator('d')
    def validate_c(**kwargs):
        pass

    @validator('e')
    def validate_e<error descr="Method must have a first parameter, usually called 'cls'">()</error>:
        pass

    def dummy(self):
        pass

    @check
    def task(self):
        pass