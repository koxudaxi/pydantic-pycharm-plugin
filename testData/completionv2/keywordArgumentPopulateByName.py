

from pydantic import BaseModel, Field

class A(BaseModel):
    model_config = ConfigDict(
        populate_by_name=True,
    )
    abc: str = Field(..., alias='ABC')
    cde: str = Field(..., alias='CDE')


A(<caret>)