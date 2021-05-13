from pydantic import BaseModel


class A(BaseModel):
    abc: str = '123'
    def __init__(self, xyz: int):
        pass

A(<caret>)