from pydantic import BaseModel
from sqlalchemy.orm.attributes import InstrumentedAttribute


class B:
    pass


class A(BaseModel):
    value: InstrumentedAttribute[str]


value: InstrumentedAttribute[int]


A(<warning descr="Field is of type 'InstrumentedAttribute[str]', 'InstrumentedAttribute[int]' may not be parsable to 'InstrumentedAttribute[str]'">value=value</warning>)
A(<warning descr="Expected type 'InstrumentedAttribute[str]', got 'B' instead">value=B()</warning>)
