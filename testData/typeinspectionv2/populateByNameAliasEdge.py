from pydantic import BaseModel, ConfigDict, Field


class Model(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    field_name: str = Field(..., alias="ALIAS_NAME")


c = Model(<warning descr="null">**{"ALIAS_NAME": "test"})</warning>
