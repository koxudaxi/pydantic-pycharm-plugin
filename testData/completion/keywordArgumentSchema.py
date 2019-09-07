from builtins import *
from pydantic import BaseModel, Schema

def get_alias():
    return 'alias_c_id'
b_id: str = 'alias_b_id'
class A(BaseModel):
    abc: str = Schema(...)
    cde = Schema(str('abc'))
    efg = Schema(default=str('abc'))
    hij = Schema(default=...)
    a_id: str = Schema(..., alias='alias_a_id')
    b_id: str = Schema(..., alias=b_id)
    c_id: str = Schema(..., alias=get_alias())
    d_id: str = Schema(..., alias=)
    e_id: str = Schema(..., alias=broken)
    f_id: str = Schema(..., alias=123)
class B(A):
    hij: str

A(<caret>)