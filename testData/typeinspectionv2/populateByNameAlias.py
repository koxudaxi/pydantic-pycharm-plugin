from pydantic import BaseModel, ConfigDict, Field


class Model(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    field_name: str = Field(..., alias="ALIAS_NAME")


a = Model(field_name="hello")
b = Model(ALIAS_NAME="world")

e = Model(<warning descr="null">)</warning>
