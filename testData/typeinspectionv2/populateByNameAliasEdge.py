from pydantic import BaseModel, ConfigDict, Field


class Model(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    field_name: str = Field(..., alias="ALIAS_NAME")


class ReservedModel(BaseModel):
    model_config: int = <warning descr="Expected type 'int', got 'None' instead">None</warning>


c = Model(**{"ALIAS_NAME": "test"})
