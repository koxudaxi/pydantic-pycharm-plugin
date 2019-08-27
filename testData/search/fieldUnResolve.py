from pydantic import BaseModel, validator


class A(BaseModel):
    cd<caret>e: str # expected


A(abc='cde')
# count 1