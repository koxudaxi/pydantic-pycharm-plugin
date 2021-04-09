

from pydantic import BaseModel, Field

class A(BaseModel):
    class Config:
        allow_population_by_field_name = True

class B(A):
    abc: str = Field(..., alias='ABC')
    cde: str = Field(..., alias='CDE')

B(<caret>)