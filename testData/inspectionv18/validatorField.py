from pydantic import BaseModel, validator


class A(BaseModel):
    foo: int
    bar: float

    _validate_stuff = validator(
        "bar",
        allow_reuse=True,
    )(do_stuff)


class B(BaseModel):
    foo: int
    bar: float

    _validate_stuff = validator(
        <error descr="Cannot find field 'abc'">"abc"</error>,
        allow_reuse=True,
    )(do_stuff)
