from builtins import *
from pydantic import BaseModel, Field

def get_alias():
    return 'alias_c_id'
b_id = 'alias_b_id'
class A(BaseModel):
    abc: str = Field(...)
    cde = Field(str('abc'))
    efg = Field(default=str('abc'))
    hij = Field(default=...)
    a_id: str = Field(..., alias='alias_a_id')
    b_id: str = Field(..., alias=b_id)
    c_id: str = Field(..., alias=get_alias())
    d_id: str = Field(..., alias=)
    e_id: str = Field(..., alias=broken)
    f_id: str = Field(..., alias=123)
    g_id: str = get_alias()
class B(A):
    hij: str

A().<caret>