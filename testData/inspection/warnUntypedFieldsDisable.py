from pydantic import BaseModel


class A(BaseModel):
    a = '123'


class B(BaseModel):
    b: str = '123'
