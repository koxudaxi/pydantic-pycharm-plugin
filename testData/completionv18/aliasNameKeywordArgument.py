from pydantic import BaseModel, Field


class A(BaseModel):
    abc: str = '123'
    efg: str = Field(alias='e-f-g')
    hij: str = Field(alias='klm')

A(<caret>)
