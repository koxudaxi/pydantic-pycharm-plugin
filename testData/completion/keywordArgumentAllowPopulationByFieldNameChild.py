

from pydantic import BaseModel, Field

class A(BaseModel):
    abc: str = Field(..., alias='ABC')
    cde: str = Field(..., alias='CDE')

class B(A):
    class Config:
        allow_population_by_field_name = True

B(<caret>)