from pydantic import BaseModel, Field


class Model(BaseModel):
    abc: str = Field(regex='<caret>[^a-zA-Z]+')
