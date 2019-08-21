from pydantic import BaseModel


class A(BaseModel):
    a: str


A(<warning descr="class 'A' accepts only keyword arguments">'a'</warning>)
