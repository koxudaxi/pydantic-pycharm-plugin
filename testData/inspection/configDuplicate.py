from pydantic import BaseModel


class E(BaseModel, allow_mutation=True):
    class Config:
        allow_mutation= True

class F(BaseModel, allow_mutation=True):
    pass

class G(BaseModel):
    class Config:
        allow_mutation=True
