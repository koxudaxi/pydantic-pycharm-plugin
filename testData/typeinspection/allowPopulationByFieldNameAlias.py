from pydantic import BaseModel, Field


class Model(BaseModel):
    field_name: str = Field(..., alias="ALIAS_NAME")

    class Config:
        allow_population_by_field_name = True


a = Model(field_name="hello")
b = Model(ALIAS_NAME="world")

e = Model(<warning descr="null">)</warning>
