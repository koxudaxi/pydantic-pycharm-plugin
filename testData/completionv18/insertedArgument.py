from pydantic import BaseModel, Field


class A(BaseModel):
    abc_efg: str = '123'
    abc_xyz: str = '456'

A(abc<caret>_efg='789')
