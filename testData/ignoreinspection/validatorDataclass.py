from pydantic import validator
from pydantic.dataclasses import dataclass

@dataclass
class A:
    a: str

    @validator('a')
    def vali<caret>date_a(cls):
        pass
