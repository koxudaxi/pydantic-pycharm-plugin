from dataclasses import field

from pydantic import BaseModel, Field
from pydantic.dataclasses import dataclass

def get_int() -> int:
    pass

def get_str() -> str:
    pass

class A(BaseModel):
    a: int = Field(default_factory=lambda: 1)
    b: str = Field(<warning descr="Expected type 'str', 'int' is set as return value of default_factory">default_factory=lambda: 1</warning>)
    c: str = Field(default_factory=get_str)
    d: str = Field(<warning descr="Expected type 'str', 'int' is set as return value of default_factory">default_factory=get_int</warning>)

@dataclass
class B:
    a: int = Field(default_factory=lambda: 1)
    b: str = Field(<warning descr="Expected type 'str', 'int' is set as return value of default_factory">default_factory=lambda: 1</warning>)
    c: str = Field(default_factory=get_str)
    d: str = Field(<warning descr="Expected type 'str', 'int' is set as return value of default_factory">default_factory=get_int</warning>)
    e: int = field(default_factory=lambda: 1)
    f: str = field(<warning descr="Expected type 'str', 'int' is set as return value of default_factory">default_factory=lambda: 1</warning>)
    g: str = Field(default_factory=get_str)
    h: str = field(<warning descr="Expected type 'str', 'int' is set as return value of default_factory">default_factory=get_int</warning>)
