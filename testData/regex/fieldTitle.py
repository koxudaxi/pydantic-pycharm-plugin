from pydantic import BaseModel, Field


class Model(BaseModel):
    abc: str = Field(title='<caret>[^a-zA-Z]+')
