from typing import Type

from pydantic import BaseModel


class A(BaseModel):
    abc: str


def get_model() -> Type[A]:
    return A


get_model()(ab<caret>c='value')
