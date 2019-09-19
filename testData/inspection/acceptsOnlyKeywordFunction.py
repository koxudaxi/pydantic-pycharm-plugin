from pydantic import BaseModel


class A(BaseModel):
    a: str


def abc(a) -> A:
    pass

abc('a')
