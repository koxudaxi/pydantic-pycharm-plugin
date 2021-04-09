

from pydantic import BaseModel


class A(BaseModel):
    a = int(123)


A(a=int(123))
