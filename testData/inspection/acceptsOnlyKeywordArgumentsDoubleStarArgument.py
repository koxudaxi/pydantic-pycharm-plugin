from pydantic import BaseModel


class A(BaseModel):
    a: str


A(**{'a':'a'})
