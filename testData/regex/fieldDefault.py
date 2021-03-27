from pydantic import BaseModel, Field


class Model(BaseModel):
    abc: str = Field('<caret>[^a-zA-Z]+')
