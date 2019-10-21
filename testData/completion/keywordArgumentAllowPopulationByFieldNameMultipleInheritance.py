from builtins import *

from pydantic import BaseModel, Field

class A(BaseModel):
    class Config:
        allow_population_by_field_name = True

class B(BaseModel):
    class Config:
        allow_population_by_field_name = False

class C(A, B):
    abc: str = Field(..., alias='ABC')
    cde: str = Field(..., alias='CDE')

C(<caret>)