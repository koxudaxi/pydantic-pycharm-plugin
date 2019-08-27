from pydantic import BaseModel, validator


class A(BaseModel):
    cde: str


A(ab<caret>c='cde')
# count 0