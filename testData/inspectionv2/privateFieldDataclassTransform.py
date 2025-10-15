from pydantic import BaseModel


class Model(BaseModel):
    _value: int


Model()
