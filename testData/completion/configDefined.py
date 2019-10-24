from builtins import *
from pydantic import BaseModel

class A(BaseModel):
    class Config:
        allow_population_by_field_name = True
        max_anystr_length = 10
        <caret>