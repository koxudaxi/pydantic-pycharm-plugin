from pydantic import BaseModel, <warning descr="Pydantic V2 Migration Guide: https://docs.pydantic.dev/dev-v2/migration/">validator</warning>, <warning descr="Pydantic V2 Migration Guide: https://docs.pydantic.dev/dev-v2/migration/">root_validator</warning>

def check(func):
    def inner():
        func()
    return inner

class A(BaseModel):
    a: str


    @<warning descr="Pydantic V2 Migration Guide: https://docs.pydantic.dev/dev-v2/migration/">validator</warning>('a')
    def validate_a(cls):
        pass

    @<warning descr="Pydantic V2 Migration Guide: https://docs.pydantic.dev/dev-v2/migration/">root_validator</warning>()
    def validate_root(cls):
        pass


    def dummy(self):
        pass

    @check
    def task(self):
        pass