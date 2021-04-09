

from pydantic import BaseModel


class A(BaseModel):
    a: int

class B(A):
    pass

B(a=int(123))
