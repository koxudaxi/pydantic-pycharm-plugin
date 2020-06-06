from pydantic import BaseModel
from pydantic.dataclasses import dataclass


class A(BaseModel):
    a: str


<warning descr="Insert required arguments">A(<warning descr="class 'A' accepts only keyword arguments">*['a']</warning>)</warning>


@dataclass
class B:
    a: str


B(*['a'])
