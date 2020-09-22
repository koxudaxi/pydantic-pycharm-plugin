from pydantic import BaseModel, root_validator

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

    @root_validator('a')
    def validate_a(<weak_warning descr="Usually first parameter of such methods is named 'cls'">self</weak_warning>):
        pass

    @root_validator('b')
    def validate_b(<weak_warning descr="Usually first parameter of such methods is named 'cls'">fles</weak_warning>):
        pass

    @root_validator('c')
    def validate_b(*args):
        pass

    @root_validator('d')
    def validate_c(**kwargs):
        pass

    @root_validator('e')
    def validate_e<error descr="Method must have a first parameter, usually called 'cls'">()</error>:
        pass

    def dummy(self):
        pass

    @check
    def task(self):
        pass

from pydantic.class_validators import root_validator

class B(BaseModel):
    a: str
    b: str
    c: str
    d: str
    e: str

    @root_validator('a')
    def validate_a(<weak_warning descr="Usually first parameter of such methods is named 'cls'">self</weak_warning>):
        pass

    @root_validator('b')
    def validate_b(<weak_warning descr="Usually first parameter of such methods is named 'cls'">fles</weak_warning>):
        pass

    @root_validator('c')
    def validate_b(*args):
        pass

    @root_validator('d')
    def validate_c(**kwargs):
        pass

    @root_validator('e')
    def validate_e<error descr="Method must have a first parameter, usually called 'cls'">()</error>:
        pass

    def dummy(self):
        pass

    @check
    def task(self):
        pass