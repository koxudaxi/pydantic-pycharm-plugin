from pydantic import BaseModel
from pydantic.dataclasses import dataclass

class A(BaseModel):
    a: str


A(<warning descr="class 'A' accepts only keyword arguments">'a'</warning><warning descr="null">)</warning>

@dataclass
class B():
    a: str


B('a')
